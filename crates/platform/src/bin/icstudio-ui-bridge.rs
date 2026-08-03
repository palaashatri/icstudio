use icstudio_project::ProjectStore;
use std::path::PathBuf;

fn main() {
    if let Err(error) = run() {
        eprintln!("icstudio-ui-bridge: {error}");
        std::process::exit(1);
    }
}

fn run() -> Result<(), String> {
    let mut args = std::env::args().skip(1);
    let mut project_path: Option<PathBuf> = None;

    while let Some(argument) = args.next() {
        match argument.as_str() {
            "--path" => {
                let value = args
                    .next()
                    .ok_or_else(|| "--path requires a value".to_string())?;
                if project_path.replace(PathBuf::from(value)).is_some() {
                    return Err("--path may only be supplied once".to_string());
                }
            }
            "--help" | "-h" => {
                println!("Usage: icstudio-ui-bridge --path PATH");
                return Ok(());
            }
            other => return Err(format!("unexpected argument '{other}'")),
        }
    }

    let project_path = project_path.ok_or_else(|| "missing required --path".to_string())?;
    let store = ProjectStore::open(project_path)?;
    println!("{}", store.project().summary_json());
    Ok(())
}
