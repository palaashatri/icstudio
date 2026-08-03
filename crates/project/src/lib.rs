//! Authoritative ICStudio project hierarchy, transactions, persistence, and recovery.
//!
//! M1 deliberately keeps the format small and independently reviewable. Metadata is
//! stored in a deterministic line-oriented text format. Every acknowledged edit is
//! journaled before the authoritative snapshot is replaced, and recovery replays only
//! complete `.ready` journal records.

use std::collections::BTreeMap;
use std::fmt;
use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};

const FORMAT_MAGIC: &str = "ICSTUDIO_PROJECT";
const FORMAT_VERSION: u32 = 1;
const STATE_DIR: &str = ".icstudio";
const SNAPSHOT_FILE: &str = "project.icst";
const SNAPSHOT_TEMP_FILE: &str = "project.icst.tmp";
const JOURNAL_DIR: &str = "journal";

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct ObjectId(u128);

impl ObjectId {
    pub fn from_u128(value: u128) -> Self {
        Self(value)
    }

    pub fn as_u128(self) -> u128 {
        self.0
    }

    pub fn to_hex(self) -> String {
        format!("{:032x}", self.0)
    }

    pub fn parse_hex(value: &str) -> Result<Self, String> {
        if value.len() != 32 || !value.bytes().all(|byte| byte.is_ascii_hexdigit()) {
            return Err(format!("invalid 128-bit object id '{value}'"));
        }
        u128::from_str_radix(value, 16)
            .map(Self)
            .map_err(|error| format!("invalid 128-bit object id '{value}': {error}"))
    }
}

impl fmt::Display for ObjectId {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(formatter, "{:032x}", self.0)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct View {
    pub id: ObjectId,
    pub name: String,
    pub kind: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Cell {
    pub id: ObjectId,
    pub name: String,
    pub views: BTreeMap<String, View>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Library {
    pub id: ObjectId,
    pub name: String,
    pub cells: BTreeMap<String, Cell>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Project {
    pub schema_version: u32,
    pub id: ObjectId,
    pub name: String,
    pub revision: u64,
    pub libraries: BTreeMap<String, Library>,
}

impl Project {
    pub fn new(name: impl Into<String>) -> Result<Self, String> {
        let name = name.into();
        validate_component("project", &name)?;
        let entropy = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map_err(|error| format!("system clock is before UNIX epoch: {error}"))?
            .as_nanos()
            ^ u128::from(std::process::id());
        let id = hash128(format!("project:{name}:{entropy}").as_bytes());
        Ok(Self {
            schema_version: FORMAT_VERSION,
            id,
            name,
            revision: 0,
            libraries: BTreeMap::new(),
        })
    }

    pub fn hierarchy_counts(&self) -> HierarchyCounts {
        let libraries = self.libraries.len();
        let cells = self
            .libraries
            .values()
            .map(|library| library.cells.len())
            .sum();
        let views = self
            .libraries
            .values()
            .flat_map(|library| library.cells.values())
            .map(|cell| cell.views.len())
            .sum();
        HierarchyCounts {
            libraries,
            cells,
            views,
        }
    }

    pub fn summary_json(&self) -> String {
        let counts = self.hierarchy_counts();
        format!(
            "{{\"schemaVersion\":{},\"projectId\":\"{}\",\"name\":\"{}\",\"revision\":{},\"libraries\":{},\"cells\":{},\"views\":{}}}",
            self.schema_version,
            self.id,
            escape_json(&self.name),
            self.revision,
            counts.libraries,
            counts.cells,
            counts.views
        )
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct HierarchyCounts {
    pub libraries: usize,
    pub cells: usize,
    pub views: usize,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Mutation {
    AddLibrary {
        name: String,
    },
    AddCell {
        library: String,
        name: String,
    },
    AddView {
        library: String,
        cell: String,
        name: String,
        kind: String,
    },
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Transaction {
    pub expected_revision: u64,
    pub request_id: String,
    pub actor: String,
    pub mutations: Vec<Mutation>,
}

impl Transaction {
    pub fn new(
        expected_revision: u64,
        request_id: impl Into<String>,
        actor: impl Into<String>,
    ) -> Self {
        Self {
            expected_revision,
            request_id: request_id.into(),
            actor: actor.into(),
            mutations: Vec::new(),
        }
    }

    pub fn add_library(mut self, name: impl Into<String>) -> Self {
        self.mutations
            .push(Mutation::AddLibrary { name: name.into() });
        self
    }

    pub fn add_cell(mut self, library: impl Into<String>, name: impl Into<String>) -> Self {
        self.mutations.push(Mutation::AddCell {
            library: library.into(),
            name: name.into(),
        });
        self
    }

    pub fn add_view(
        mut self,
        library: impl Into<String>,
        cell: impl Into<String>,
        name: impl Into<String>,
        kind: impl Into<String>,
    ) -> Self {
        self.mutations.push(Mutation::AddView {
            library: library.into(),
            cell: cell.into(),
            name: name.into(),
            kind: kind.into(),
        });
        self
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CommitFailpoint {
    AfterJournalReady,
    AfterSnapshotTemp,
}

#[derive(Debug)]
pub struct ProjectStore {
    root: PathBuf,
    project: Project,
}

impl ProjectStore {
    pub fn create(root: impl AsRef<Path>, name: impl Into<String>) -> Result<Self, String> {
        let root = root.as_ref().to_path_buf();
        fs::create_dir_all(state_path(&root))
            .map_err(|error| format!("failed to create project directory: {error}"))?;
        fs::create_dir_all(journal_path(&root))
            .map_err(|error| format!("failed to create project journal: {error}"))?;
        let snapshot = snapshot_path(&root);
        if snapshot.exists() {
            return Err(format!("project already exists at {}", root.display()));
        }
        let project = Project::new(name)?;
        write_snapshot(&root, &serialize_project(&project))?;
        Ok(Self { root, project })
    }

    pub fn open(root: impl AsRef<Path>) -> Result<Self, String> {
        let root = root.as_ref().to_path_buf();
        recover(&root)?;
        let snapshot = snapshot_path(&root);
        let text = fs::read_to_string(&snapshot)
            .map_err(|error| format!("failed to read {}: {error}", snapshot.display()))?;
        let project = deserialize_project(&text)?;
        Ok(Self { root, project })
    }

    pub fn project(&self) -> &Project {
        &self.project
    }

    pub fn root(&self) -> &Path {
        &self.root
    }

    pub fn commit(&mut self, transaction: Transaction) -> Result<u64, String> {
        self.commit_internal(transaction, None)
    }

    pub fn commit_with_failpoint(
        &mut self,
        transaction: Transaction,
        failpoint: CommitFailpoint,
    ) -> Result<u64, String> {
        self.commit_internal(transaction, Some(failpoint))
    }

    fn commit_internal(
        &mut self,
        transaction: Transaction,
        failpoint: Option<CommitFailpoint>,
    ) -> Result<u64, String> {
        let next = apply_transaction(&self.project, &transaction)?;
        let serialized = serialize_project(&next);
        let journal_dir = journal_path(&self.root);
        fs::create_dir_all(&journal_dir)
            .map_err(|error| format!("failed to create {}: {error}", journal_dir.display()))?;
        let journal_temp = journal_dir.join(format!("revision-{:020}.tmp", next.revision));
        let journal_ready = journal_dir.join(format!("revision-{:020}.ready", next.revision));
        write_synced(&journal_temp, &serialized)?;
        if journal_ready.exists() {
            fs::remove_file(&journal_ready).map_err(|error| {
                format!("failed to replace {}: {error}", journal_ready.display())
            })?;
        }
        fs::rename(&journal_temp, &journal_ready).map_err(|error| {
            format!(
                "failed to publish journal {}: {error}",
                journal_ready.display()
            )
        })?;

        if failpoint == Some(CommitFailpoint::AfterJournalReady) {
            return Err("injected termination after journal publication".to_string());
        }

        let snapshot_temp = state_path(&self.root).join(SNAPSHOT_TEMP_FILE);
        write_synced(&snapshot_temp, &serialized)?;
        if failpoint == Some(CommitFailpoint::AfterSnapshotTemp) {
            return Err("injected termination after snapshot staging".to_string());
        }
        replace_file(&snapshot_temp, &snapshot_path(&self.root))?;
        fs::remove_file(&journal_ready).map_err(|error| {
            format!(
                "failed to retire committed journal {}: {error}",
                journal_ready.display()
            )
        })?;
        self.project = next;
        Ok(self.project.revision)
    }
}

pub fn apply_transaction(project: &Project, transaction: &Transaction) -> Result<Project, String> {
    if transaction.expected_revision != project.revision {
        return Err(format!(
            "revision conflict: expected {}, current {}",
            transaction.expected_revision, project.revision
        ));
    }
    if transaction.request_id.trim().is_empty() {
        return Err("transaction request_id must not be empty".to_string());
    }
    if transaction.actor.trim().is_empty() {
        return Err("transaction actor must not be empty".to_string());
    }
    if transaction.mutations.is_empty() {
        return Err("transaction must contain at least one mutation".to_string());
    }

    let mut next = project.clone();
    for mutation in &transaction.mutations {
        match mutation {
            Mutation::AddLibrary { name } => add_library(&mut next, name)?,
            Mutation::AddCell { library, name } => add_cell(&mut next, library, name)?,
            Mutation::AddView {
                library,
                cell,
                name,
                kind,
            } => add_view(&mut next, library, cell, name, kind)?,
        }
    }
    next.revision = next
        .revision
        .checked_add(1)
        .ok_or_else(|| "project revision overflow".to_string())?;
    Ok(next)
}

pub fn serialize_project(project: &Project) -> String {
    let mut output = String::new();
    output.push_str(&format!("{FORMAT_MAGIC}\t{FORMAT_VERSION}\n"));
    output.push_str(&format!(
        "project\t{}\t{}\t{}\n",
        project.id,
        project.revision,
        escape_field(&project.name)
    ));
    for library in project.libraries.values() {
        output.push_str(&format!(
            "library\t{}\t{}\n",
            library.id,
            escape_field(&library.name)
        ));
        for cell in library.cells.values() {
            output.push_str(&format!(
                "cell\t{}\t{}\t{}\n",
                cell.id,
                escape_field(&library.name),
                escape_field(&cell.name)
            ));
            for view in cell.views.values() {
                output.push_str(&format!(
                    "view\t{}\t{}\t{}\t{}\t{}\n",
                    view.id,
                    escape_field(&library.name),
                    escape_field(&cell.name),
                    escape_field(&view.name),
                    escape_field(&view.kind)
                ));
            }
        }
    }
    output
}

pub fn deserialize_project(input: &str) -> Result<Project, String> {
    let mut lines = input.lines();
    let header = lines
        .next()
        .ok_or_else(|| "project file is empty".to_string())?;
    let header_fields: Vec<&str> = header.split('\t').collect();
    if header_fields != [FORMAT_MAGIC, "1"] {
        return Err(format!("unsupported project header '{header}'"));
    }

    let project_line = lines
        .next()
        .ok_or_else(|| "project record is missing".to_string())?;
    let fields: Vec<&str> = project_line.split('\t').collect();
    if fields.len() != 4 || fields[0] != "project" {
        return Err("invalid project record".to_string());
    }
    let mut project = Project {
        schema_version: FORMAT_VERSION,
        id: ObjectId::parse_hex(fields[1])?,
        revision: fields[2]
            .parse::<u64>()
            .map_err(|error| format!("invalid project revision '{}': {error}", fields[2]))?,
        name: unescape_field(fields[3])?,
        libraries: BTreeMap::new(),
    };
    validate_component("project", &project.name)?;

    for (line_index, line) in lines.enumerate() {
        if line.is_empty() {
            continue;
        }
        let fields: Vec<&str> = line.split('\t').collect();
        match fields.first().copied() {
            Some("library") if fields.len() == 3 => {
                let name = unescape_field(fields[2])?;
                validate_component("library", &name)?;
                let library = Library {
                    id: ObjectId::parse_hex(fields[1])?,
                    name: name.clone(),
                    cells: BTreeMap::new(),
                };
                if project.libraries.insert(name.clone(), library).is_some() {
                    return Err(format!("duplicate library '{name}'"));
                }
            }
            Some("cell") if fields.len() == 4 => {
                let library_name = unescape_field(fields[2])?;
                let cell_name = unescape_field(fields[3])?;
                validate_component("cell", &cell_name)?;
                let library = project
                    .libraries
                    .get_mut(&library_name)
                    .ok_or_else(|| format!("cell references missing library '{library_name}'"))?;
                let cell = Cell {
                    id: ObjectId::parse_hex(fields[1])?,
                    name: cell_name.clone(),
                    views: BTreeMap::new(),
                };
                if library.cells.insert(cell_name.clone(), cell).is_some() {
                    return Err(format!("duplicate cell '{library_name}/{cell_name}'"));
                }
            }
            Some("view") if fields.len() == 6 => {
                let library_name = unescape_field(fields[2])?;
                let cell_name = unescape_field(fields[3])?;
                let view_name = unescape_field(fields[4])?;
                let kind = unescape_field(fields[5])?;
                validate_component("view", &view_name)?;
                validate_view_kind(&kind)?;
                let cell = project
                    .libraries
                    .get_mut(&library_name)
                    .and_then(|library| library.cells.get_mut(&cell_name))
                    .ok_or_else(|| {
                        format!("view references missing cell '{library_name}/{cell_name}'")
                    })?;
                let view = View {
                    id: ObjectId::parse_hex(fields[1])?,
                    name: view_name.clone(),
                    kind,
                };
                if cell.views.insert(view_name.clone(), view).is_some() {
                    return Err(format!(
                        "duplicate view '{library_name}/{cell_name}/{view_name}'"
                    ));
                }
            }
            _ => {
                return Err(format!(
                    "invalid project record at line {}: '{line}'",
                    line_index + 3
                ));
            }
        }
    }
    Ok(project)
}

pub fn project_snapshot_path(root: &Path) -> PathBuf {
    snapshot_path(root)
}

fn add_library(project: &mut Project, name: &str) -> Result<(), String> {
    validate_component("library", name)?;
    if project.libraries.contains_key(name) {
        return Err(format!("library '{name}' already exists"));
    }
    let id = derive_id(project.id, &format!("library/{name}"));
    project.libraries.insert(
        name.to_string(),
        Library {
            id,
            name: name.to_string(),
            cells: BTreeMap::new(),
        },
    );
    Ok(())
}

fn add_cell(project: &mut Project, library_name: &str, cell_name: &str) -> Result<(), String> {
    validate_component("library", library_name)?;
    validate_component("cell", cell_name)?;
    let library = project
        .libraries
        .get_mut(library_name)
        .ok_or_else(|| format!("library '{library_name}' does not exist"))?;
    if library.cells.contains_key(cell_name) {
        return Err(format!("cell '{library_name}/{cell_name}' already exists"));
    }
    let id = derive_id(library.id, &format!("cell/{cell_name}"));
    library.cells.insert(
        cell_name.to_string(),
        Cell {
            id,
            name: cell_name.to_string(),
            views: BTreeMap::new(),
        },
    );
    Ok(())
}

fn add_view(
    project: &mut Project,
    library_name: &str,
    cell_name: &str,
    view_name: &str,
    kind: &str,
) -> Result<(), String> {
    validate_component("library", library_name)?;
    validate_component("cell", cell_name)?;
    validate_component("view", view_name)?;
    validate_view_kind(kind)?;
    let cell = project
        .libraries
        .get_mut(library_name)
        .and_then(|library| library.cells.get_mut(cell_name))
        .ok_or_else(|| format!("cell '{library_name}/{cell_name}' does not exist"))?;
    if cell.views.contains_key(view_name) {
        return Err(format!(
            "view '{library_name}/{cell_name}/{view_name}' already exists"
        ));
    }
    let id = derive_id(cell.id, &format!("view/{view_name}/{kind}"));
    cell.views.insert(
        view_name.to_string(),
        View {
            id,
            name: view_name.to_string(),
            kind: kind.to_string(),
        },
    );
    Ok(())
}

fn recover(root: &Path) -> Result<(), String> {
    let state = state_path(root);
    let journal = journal_path(root);
    fs::create_dir_all(&journal)
        .map_err(|error| format!("failed to create {}: {error}", journal.display()))?;

    let snapshot = snapshot_path(root);
    let current_revision = if snapshot.exists() {
        let text = fs::read_to_string(&snapshot)
            .map_err(|error| format!("failed to read {}: {error}", snapshot.display()))?;
        Some(deserialize_project(&text)?.revision)
    } else {
        None
    };

    let mut ready_records = Vec::new();
    for entry in fs::read_dir(&journal)
        .map_err(|error| format!("failed to read {}: {error}", journal.display()))?
    {
        let entry = entry.map_err(|error| format!("failed to read journal entry: {error}"))?;
        let path = entry.path();
        match path.extension().and_then(|value| value.to_str()) {
            Some("ready") => {
                let text = fs::read_to_string(&path)
                    .map_err(|error| format!("failed to read {}: {error}", path.display()))?;
                let project = deserialize_project(&text)?;
                ready_records.push((project.revision, path, text));
            }
            Some("tmp") => {
                fs::remove_file(&path).map_err(|error| {
                    format!(
                        "failed to remove incomplete journal {}: {error}",
                        path.display()
                    )
                })?;
            }
            _ => {}
        }
    }
    ready_records.sort_by_key(|record| record.0);
    if let Some((revision, _, text)) = ready_records.last() {
        if current_revision.is_none_or(|current| *revision > current) {
            write_snapshot(root, text)?;
        }
    }
    for (_, path, _) in ready_records {
        fs::remove_file(&path)
            .map_err(|error| format!("failed to remove {}: {error}", path.display()))?;
    }

    let staged_snapshot = state.join(SNAPSHOT_TEMP_FILE);
    if staged_snapshot.exists() {
        fs::remove_file(&staged_snapshot).map_err(|error| {
            format!(
                "failed to remove staged snapshot {}: {error}",
                staged_snapshot.display()
            )
        })?;
    }
    if !snapshot.exists() {
        return Err(format!("no project exists at {}", root.display()));
    }
    Ok(())
}

fn write_snapshot(root: &Path, contents: &str) -> Result<(), String> {
    let state = state_path(root);
    fs::create_dir_all(&state)
        .map_err(|error| format!("failed to create {}: {error}", state.display()))?;
    let temporary = state.join(SNAPSHOT_TEMP_FILE);
    write_synced(&temporary, contents)?;
    replace_file(&temporary, &snapshot_path(root))
}

fn write_synced(path: &Path, contents: &str) -> Result<(), String> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .map_err(|error| format!("failed to create {}: {error}", parent.display()))?;
    }
    let mut file = fs::File::create(path)
        .map_err(|error| format!("failed to create {}: {error}", path.display()))?;
    file.write_all(contents.as_bytes())
        .map_err(|error| format!("failed to write {}: {error}", path.display()))?;
    file.sync_all()
        .map_err(|error| format!("failed to sync {}: {error}", path.display()))
}

fn replace_file(source: &Path, destination: &Path) -> Result<(), String> {
    if destination.exists() {
        fs::remove_file(destination)
            .map_err(|error| format!("failed to replace {}: {error}", destination.display()))?;
    }
    fs::rename(source, destination).map_err(|error| {
        format!(
            "failed to publish {} from {}: {error}",
            destination.display(),
            source.display()
        )
    })
}

fn state_path(root: &Path) -> PathBuf {
    root.join(STATE_DIR)
}

fn snapshot_path(root: &Path) -> PathBuf {
    state_path(root).join(SNAPSHOT_FILE)
}

fn journal_path(root: &Path) -> PathBuf {
    state_path(root).join(JOURNAL_DIR)
}

fn validate_component(kind: &str, value: &str) -> Result<(), String> {
    if value.is_empty() || value.len() > 128 {
        return Err(format!("{kind} name must contain 1 to 128 bytes"));
    }
    if !value.chars().all(|character| {
        character.is_ascii_alphanumeric()
            || character == '_'
            || character == '-'
            || character == '.'
    }) {
        return Err(format!(
            "{kind} name '{value}' contains unsupported characters"
        ));
    }
    Ok(())
}

fn validate_view_kind(kind: &str) -> Result<(), String> {
    validate_component("view kind", kind)
}

fn derive_id(parent: ObjectId, path: &str) -> ObjectId {
    hash128(format!("{}:{path}", parent.to_hex()).as_bytes())
}

fn hash128(bytes: &[u8]) -> ObjectId {
    let high = fnv1a64(bytes, 0xcbf29ce484222325);
    let low = fnv1a64(bytes, 0x84222325cbf29ce4);
    ObjectId((u128::from(high) << 64) | u128::from(low))
}

fn fnv1a64(bytes: &[u8], offset: u64) -> u64 {
    let mut hash = offset;
    for byte in bytes {
        hash ^= u64::from(*byte);
        hash = hash.wrapping_mul(0x100000001b3);
    }
    hash
}

fn escape_field(value: &str) -> String {
    let mut output = String::with_capacity(value.len());
    for character in value.chars() {
        match character {
            '\\' => output.push_str("\\\\"),
            '\t' => output.push_str("\\t"),
            '\n' => output.push_str("\\n"),
            '\r' => output.push_str("\\r"),
            other => output.push(other),
        }
    }
    output
}

fn unescape_field(value: &str) -> Result<String, String> {
    let mut output = String::with_capacity(value.len());
    let mut characters = value.chars();
    while let Some(character) = characters.next() {
        if character != '\\' {
            output.push(character);
            continue;
        }
        let escaped = characters
            .next()
            .ok_or_else(|| "unterminated project field escape".to_string())?;
        match escaped {
            '\\' => output.push('\\'),
            't' => output.push('\t'),
            'n' => output.push('\n'),
            'r' => output.push('\r'),
            other => return Err(format!("unsupported project field escape '\\{other}'")),
        }
    }
    Ok(output)
}

fn escape_json(value: &str) -> String {
    let mut output = String::with_capacity(value.len());
    for character in value.chars() {
        match character {
            '"' => output.push_str("\\\""),
            '\\' => output.push_str("\\\\"),
            '\n' => output.push_str("\\n"),
            '\r' => output.push_str("\\r"),
            '\t' => output.push_str("\\t"),
            other if other.is_control() => output.push_str(&format!("\\u{:04x}", other as u32)),
            other => output.push(other),
        }
    }
    output
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temporary_root(label: &str) -> PathBuf {
        let unique = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("clock")
            .as_nanos();
        std::env::temp_dir().join(format!(
            "icstudio-project-{label}-{}-{unique}",
            std::process::id()
        ))
    }

    fn populated_project(root: &Path) -> ProjectStore {
        let mut store = ProjectStore::create(root, "demo").expect("create project");
        let transaction = Transaction::new(0, "request-1", "test")
            .add_library("analog")
            .add_cell("analog", "inverter")
            .add_view("analog", "inverter", "schematic", "schematic")
            .add_view("analog", "inverter", "symbol", "symbol");
        store.commit(transaction).expect("commit hierarchy");
        store
    }

    #[test]
    fn create_save_reopen_preserves_hierarchy_and_ids() {
        let root = temporary_root("reopen");
        let store = populated_project(&root);
        let expected = store.project().clone();
        drop(store);

        let reopened = ProjectStore::open(&root).expect("reopen project");
        assert_eq!(reopened.project(), &expected);
        assert_eq!(
            reopened.project().hierarchy_counts(),
            HierarchyCounts {
                libraries: 1,
                cells: 1,
                views: 2
            }
        );
        fs::remove_dir_all(root).expect("cleanup");
    }

    #[test]
    fn serialization_round_trip_is_deterministic() {
        let root = temporary_root("roundtrip");
        let store = populated_project(&root);
        let first = serialize_project(store.project());
        let decoded = deserialize_project(&first).expect("decode project");
        let second = serialize_project(&decoded);
        assert_eq!(first, second);
        fs::remove_dir_all(root).expect("cleanup");
    }

    #[test]
    fn ready_journal_recovers_after_injected_termination() {
        let root = temporary_root("recovery");
        let mut store = ProjectStore::create(&root, "recovery").expect("create project");
        let transaction = Transaction::new(0, "request-recovery", "test").add_library("recovered");
        let error = store
            .commit_with_failpoint(transaction, CommitFailpoint::AfterJournalReady)
            .expect_err("failpoint must interrupt commit");
        assert!(error.contains("injected termination"));
        drop(store);

        let recovered = ProjectStore::open(&root).expect("recover project");
        assert_eq!(recovered.project().revision, 1);
        assert!(recovered.project().libraries.contains_key("recovered"));
        fs::remove_dir_all(root).expect("cleanup");
    }

    #[test]
    fn stale_revision_is_rejected_without_mutation() {
        let root = temporary_root("conflict");
        let mut store = populated_project(&root);
        let before = store.project().clone();
        let stale = Transaction::new(0, "request-stale", "test").add_library("stale");
        let error = store.commit(stale).expect_err("stale transaction");
        assert!(error.contains("revision conflict"));
        assert_eq!(store.project(), &before);
        fs::remove_dir_all(root).expect("cleanup");
    }
}
