use icstudio_platform::{
    capability_report_markdown, create_checkpoint, license_check, reported_truth_score,
    resume_check, truth_score, validate_project_state, write_sbom, write_text,
};
use icstudio_project::{ProjectStore, Transaction};
use std::path::{Path, PathBuf};

fn main() {
    if let Err(error) = run() {
        eprintln!("icstudio: {error}");
        std::process::exit(1);
    }
}

fn run() -> Result<(), String> {
    let mut args: Vec<String> = std::env::args().skip(1).collect();
    let root = take_option(&mut args, "--project-root")?
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("."));
    let command = args
        .first()
        .cloned()
        .ok_or_else(|| usage("missing command"))?;
    args.remove(0);

    match command.as_str() {
        "project" => run_project_command(&root, args)?,
        "validate" => {
            require_empty(&args)?;
            validate_project_state(&root)?;
            println!("project state: valid");
        }
        "truth" => {
            require_empty(&args)?;
            let computed = truth_score(&root)?;
            let reported = reported_truth_score(&root)?;
            println!("ICStudio truth score: {computed:.2}/100");
            println!("reported score: {reported:.2}/100");
            println!("assessment: M0 accepted; M1 project database foundation in development");
        }
        "capabilities" => {
            let output = take_option(&mut args, "--output")?.map(PathBuf::from);
            require_empty(&args)?;
            let report = capability_report_markdown(&root)?;
            if let Some(path) = output {
                write_text(&root.join(path), &report)?;
            } else {
                print!("{report}");
            }
        }
        "checkpoint" => {
            let name = take_option(&mut args, "--name")?
                .ok_or_else(|| usage("checkpoint requires --name"))?;
            require_empty(&args)?;
            let path = create_checkpoint(&root, &name)?;
            println!("created checkpoint {}", path.display());
        }
        "resume-check" => {
            let checkpoint = take_option(&mut args, "--checkpoint")?
                .ok_or_else(|| usage("resume-check requires --checkpoint"))?;
            require_empty(&args)?;
            resume_check(&root, &checkpoint)?;
            println!("checkpoint {checkpoint}: compatible");
        }
        "license-check" => {
            require_empty(&args)?;
            license_check(&root)?;
            println!("licence policy: MIT core, no external Rust dependencies");
        }
        "sbom" => {
            let output = take_option(&mut args, "--output")?
                .ok_or_else(|| usage("sbom requires --output"))?;
            require_empty(&args)?;
            let destination = resolve_path(&root, Path::new(&output));
            write_sbom(&root, &destination)?;
            println!("wrote SPDX SBOM to {}", destination.display());
        }
        "help" | "--help" | "-h" => println!("{}", usage_text()),
        other => return Err(usage(&format!("unknown command '{other}'"))),
    }

    Ok(())
}

fn run_project_command(root: &Path, mut args: Vec<String>) -> Result<(), String> {
    let command = args
        .first()
        .cloned()
        .ok_or_else(|| usage("project requires a subcommand"))?;
    args.remove(0);
    let path = take_option(&mut args, "--path")?
        .map(PathBuf::from)
        .ok_or_else(|| usage("project command requires --path"))?;
    let path = resolve_path(root, &path);

    match command.as_str() {
        "create" => {
            let name = take_option(&mut args, "--name")?
                .ok_or_else(|| usage("project create requires --name"))?;
            require_empty(&args)?;
            let store = ProjectStore::create(&path, name)?;
            println!("{}", store.project().summary_json());
        }
        "show" => {
            require_empty(&args)?;
            let store = ProjectStore::open(&path)?;
            println!("{}", store.project().summary_json());
        }
        "add-library" => {
            let name = take_option(&mut args, "--name")?
                .ok_or_else(|| usage("project add-library requires --name"))?;
            let expected_revision = required_revision(&mut args)?;
            let request_id = request_id(&mut args, expected_revision)?;
            let actor = actor(&mut args)?;
            require_empty(&args)?;
            let mut store = ProjectStore::open(&path)?;
            let transaction =
                Transaction::new(expected_revision, request_id, actor).add_library(name);
            store.commit(transaction)?;
            println!("{}", store.project().summary_json());
        }
        "add-cell" => {
            let library = take_option(&mut args, "--library")?
                .ok_or_else(|| usage("project add-cell requires --library"))?;
            let name = take_option(&mut args, "--name")?
                .ok_or_else(|| usage("project add-cell requires --name"))?;
            let expected_revision = required_revision(&mut args)?;
            let request_id = request_id(&mut args, expected_revision)?;
            let actor = actor(&mut args)?;
            require_empty(&args)?;
            let mut store = ProjectStore::open(&path)?;
            let transaction =
                Transaction::new(expected_revision, request_id, actor).add_cell(library, name);
            store.commit(transaction)?;
            println!("{}", store.project().summary_json());
        }
        "add-view" => {
            let library = take_option(&mut args, "--library")?
                .ok_or_else(|| usage("project add-view requires --library"))?;
            let cell = take_option(&mut args, "--cell")?
                .ok_or_else(|| usage("project add-view requires --cell"))?;
            let name = take_option(&mut args, "--name")?
                .ok_or_else(|| usage("project add-view requires --name"))?;
            let kind = take_option(&mut args, "--kind")?
                .ok_or_else(|| usage("project add-view requires --kind"))?;
            let expected_revision = required_revision(&mut args)?;
            let request_id = request_id(&mut args, expected_revision)?;
            let actor = actor(&mut args)?;
            require_empty(&args)?;
            let mut store = ProjectStore::open(&path)?;
            let transaction = Transaction::new(expected_revision, request_id, actor)
                .add_view(library, cell, name, kind);
            store.commit(transaction)?;
            println!("{}", store.project().summary_json());
        }
        other => return Err(usage(&format!("unknown project command '{other}'"))),
    }
    Ok(())
}

fn required_revision(args: &mut Vec<String>) -> Result<u64, String> {
    let value = take_option(args, "--expected-revision")?
        .ok_or_else(|| usage("mutating project command requires --expected-revision"))?;
    value
        .parse::<u64>()
        .map_err(|error| usage(&format!("invalid expected revision '{value}': {error}")))
}

fn request_id(args: &mut Vec<String>, revision: u64) -> Result<String, String> {
    Ok(take_option(args, "--request-id")?
        .unwrap_or_else(|| format!("cli-{}-{revision}", std::process::id())))
}

fn actor(args: &mut Vec<String>) -> Result<String, String> {
    Ok(take_option(args, "--actor")?.unwrap_or_else(|| "cli".to_string()))
}

fn take_option(args: &mut Vec<String>, name: &str) -> Result<Option<String>, String> {
    let Some(index) = args.iter().position(|argument| argument == name) else {
        return Ok(None);
    };
    args.remove(index);
    if index >= args.len() || args[index].starts_with("--") {
        return Err(usage(&format!("{name} requires a value")));
    }
    Ok(Some(args.remove(index)))
}

fn require_empty(args: &[String]) -> Result<(), String> {
    if args.is_empty() {
        Ok(())
    } else {
        Err(usage(&format!("unexpected arguments: {}", args.join(" "))))
    }
}

fn resolve_path(root: &Path, output: &Path) -> PathBuf {
    if output.is_absolute() {
        output.to_path_buf()
    } else {
        root.join(output)
    }
}

fn usage(reason: &str) -> String {
    format!("{reason}\n\n{}", usage_text())
}

fn usage_text() -> &'static str {
    "Usage: icstudio [--project-root PATH] <command> [options]\n\nCommands:\n  project create --path PATH --name NAME\n  project show --path PATH\n  project add-library --path PATH --name NAME --expected-revision N [--request-id ID] [--actor ACTOR]\n  project add-cell --path PATH --library LIB --name NAME --expected-revision N [--request-id ID] [--actor ACTOR]\n  project add-view --path PATH --library LIB --cell CELL --name NAME --kind KIND --expected-revision N [--request-id ID] [--actor ACTOR]\n  validate\n  truth\n  capabilities [--output PATH]\n  checkpoint --name CP-...\n  resume-check --checkpoint CP-...\n  license-check\n  sbom --output PATH\n"
}
