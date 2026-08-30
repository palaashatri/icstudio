use icstudio_rpc::{RequestEnvelope, ResponseEnvelope};
use std::io::{self, BufRead, Write};

fn main() {
    if let Err(error) = run() {
        eprintln!("icstudio-worker: {error}");
        std::process::exit(64);
    }
}

fn run() -> Result<(), String> {
    let stdin = io::stdin();
    let mut stdout = io::stdout().lock();
    for line in stdin.lock().lines() {
        let line = line.map_err(|error| format!("failed to read request: {error}"))?;
        if line.trim().is_empty() {
            continue;
        }
        let request = RequestEnvelope::decode(&line)?;
        match request.command.as_str() {
            "worker.ping" => {
                let response = ResponseEnvelope {
                    request_id: request.request_id,
                    success: true,
                    payload: format!(
                        "project={} revision={} payload={}",
                        request.project_id, request.expected_revision, request.payload
                    ),
                };
                writeln!(stdout, "{}", response.encode()?)
                    .map_err(|error| format!("failed to write response: {error}"))?;
                stdout
                    .flush()
                    .map_err(|error| format!("failed to flush response: {error}"))?;
            }
            "worker.crash" => std::process::abort(),
            command => {
                let response = ResponseEnvelope {
                    request_id: request.request_id,
                    success: false,
                    payload: format!("unknown worker command '{command}'"),
                };
                writeln!(stdout, "{}", response.encode()?)
                    .map_err(|error| format!("failed to write response: {error}"))?;
                stdout
                    .flush()
                    .map_err(|error| format!("failed to flush response: {error}"))?;
            }
        }
    }
    Ok(())
}
