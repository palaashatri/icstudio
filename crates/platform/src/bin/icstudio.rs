use icstudio_platform::{
    capability_report_markdown, create_checkpoint, license_check, reported_truth_score,
    resume_check, truth_score, validate_project_state, write_sbom, write_text,
};
use std::path::{Path, PathBuf};

fn main() {
    if let Err(error) = run() {
        eprintln!("icstudio: {error}");
        std::process::exit(1);
    }
}

fn run() -> Result<(), String> {
    let mut args: Vec<String> = std::env::args().skip(1).collect();
    let root = take_option(&mut args, "--project-root")
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("."));
    let command = args
        .first()
        .cloned()
        .ok_or_else(|| usage("missing command"))?;
    args.remove(0);

    match command.as_str() {
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
            println!("assessment: M0 factory only; no IC design or solver parity claimed");
        }
        "capabilities" => {
            let output = take_option(&mut args, "--output").map(PathBuf::from);
            require_empty(&args)?;
            let report = capability_report_markdown(&root)?;
            if let Some(path) = output {
                write_text(&root.join(path), &report)?;
            } else {
                print!("{report}");
            }
        }
        "checkpoint" => {
            let name = take_option(&mut args, "--name")
                .ok_or_else(|| usage("checkpoint requires --name"))?;
            require_empty(&args)?;
            let path = create_checkpoint(&root, &name)?;
            println!("created checkpoint {}", path.display());
        }
        "resume-check" => {
            let checkpoint = take_option(&mut args, "--checkpoint")
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
            let output = take_option(&mut args, "--output")
                .ok_or_else(|| usage("sbom requires --output"))?;
            require_empty(&args)?;
            let destination = resolve_output(&root, Path::new(&output));
            write_sbom(&root, &destination)?;
            println!("wrote SPDX SBOM to {}", destination.display());
        }
        "help" | "--help" | "-h" => println!("{}", usage_text()),
        other => return Err(usage(&format!("unknown command '{other}'"))),
    }

    Ok(())
}

fn take_option(args: &mut Vec<String>, name: &str) -> Option<String> {
    let index = args.iter().position(|argument| argument == name)?;
    if index + 1 >= args.len() {
        return None;
    }
    args.remove(index);
    Some(args.remove(index))
}

fn require_empty(args: &[String]) -> Result<(), String> {
    if args.is_empty() {
        Ok(())
    } else {
        Err(usage(&format!("unexpected arguments: {}", args.join(" "))))
    }
}

fn resolve_output(root: &Path, output: &Path) -> PathBuf {
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
    "Usage: icstudio [--project-root PATH] <command> [options]\n\nCommands:\n  validate\n  truth\n  capabilities [--output PATH]\n  checkpoint --name CP-...\n  resume-check --checkpoint CP-...\n  license-check\n  sbom --output PATH\n"
}
