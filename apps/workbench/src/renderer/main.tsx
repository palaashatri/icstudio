import React, { useEffect, useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { decodeSnapshot, snapshotRows } from "./snapshot.mjs";
import "./styles.css";

async function drawWebGpuScene(canvas: HTMLCanvasElement): Promise<string> {
  const gpu = (navigator as Navigator & { gpu?: any }).gpu;
  if (!gpu) {
    return "WebGPU unavailable; authoritative project state remains accessible.";
  }
  const adapter = await gpu.requestAdapter({ powerPreference: "high-performance" });
  if (!adapter) {
    return "No WebGPU adapter was available.";
  }
  const device = await adapter.requestDevice();
  const context = canvas.getContext("webgpu") as any;
  if (!context) {
    return "Unable to create a WebGPU canvas context.";
  }

  const format = gpu.getPreferredCanvasFormat();
  context.configure({ device, format, alphaMode: "opaque" });
  const shader = device.createShaderModule({
    label: "M1 project scene",
    code: `
      struct VertexOut {
        @builtin(position) position: vec4f,
        @location(0) tint: vec3f,
      };

      @vertex
      fn vertexMain(@builtin(vertex_index) vertexIndex: u32) -> VertexOut {
        var positions = array<vec2f, 3>(
          vec2f(-0.72, -0.52),
          vec2f(0.72, -0.52),
          vec2f(0.0, 0.68)
        );
        var colors = array<vec3f, 3>(
          vec3f(0.18, 0.63, 1.0),
          vec3f(0.52, 0.32, 1.0),
          vec3f(0.16, 0.92, 0.72)
        );
        var output: VertexOut;
        output.position = vec4f(positions[vertexIndex], 0.0, 1.0);
        output.tint = colors[vertexIndex];
        return output;
      }

      @fragment
      fn fragmentMain(input: VertexOut) -> @location(0) vec4f {
        return vec4f(input.tint, 1.0);
      }
    `
  });
  const pipeline = device.createRenderPipeline({
    label: "M1 scene pipeline",
    layout: "auto",
    vertex: { module: shader, entryPoint: "vertexMain" },
    fragment: { module: shader, entryPoint: "fragmentMain", targets: [{ format }] },
    primitive: { topology: "triangle-list" }
  });
  const encoder = device.createCommandEncoder({ label: "M1 scene encoder" });
  const pass = encoder.beginRenderPass({
    colorAttachments: [
      {
        view: context.getCurrentTexture().createView(),
        clearValue: { r: 0.035, g: 0.055, b: 0.09, a: 1 },
        loadOp: "clear",
        storeOp: "store"
      }
    ]
  });
  pass.setPipeline(pipeline);
  pass.draw(3);
  pass.end();
  device.queue.submit([encoder.finish()]);
  return "WebGPU scene active";
}

function App(): React.JSX.Element {
  const [snapshotText, setSnapshotText] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [gpuStatus, setGpuStatus] = useState("Initializing WebGPU scene…");
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    let active = true;
    window.icstudio
      .readProjectSnapshot()
      .then((value) => {
        if (active) {
          setSnapshotText(value);
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(reason instanceof Error ? reason.message : String(reason));
        }
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    void drawWebGpuScene(canvas)
      .then(setGpuStatus)
      .catch((reason: unknown) =>
        setGpuStatus(reason instanceof Error ? reason.message : String(reason))
      );
  }, []);

  const snapshot = useMemo(() => {
    if (!snapshotText) {
      return null;
    }
    try {
      return decodeSnapshot(snapshotText);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
      return null;
    }
  }, [snapshotText]);

  return (
    <main className="workbench-shell">
      <header className="titlebar">
        <div>
          <span className="eyebrow">ICStudio</span>
          <h1>{snapshot?.name ?? "Workbench"}</h1>
        </div>
        <div className="revision-pill">
          <span>Authoritative revision</span>
          <strong>{snapshot?.revision ?? "—"}</strong>
        </div>
      </header>

      <section className="workspace-grid" aria-label="Project workbench">
        <aside className="project-panel">
          <div className="panel-heading">
            <span>Project</span>
            <span className="read-only">Immutable snapshot</span>
          </div>
          {error ? <p className="error-message">{error}</p> : null}
          {!snapshot && !error ? <p className="loading">Reading platform state…</p> : null}
          {snapshot ? (
            <>
              <dl className="summary-grid">
                {snapshotRows(snapshot).map((row) => (
                  <div key={row.label}>
                    <dt>{row.label}</dt>
                    <dd>{row.value}</dd>
                  </div>
                ))}
              </dl>
              <div className="project-id">
                <span>Project ID</span>
                <code>{snapshot.projectId}</code>
              </div>
            </>
          ) : null}
        </aside>

        <section className="scene-panel">
          <canvas ref={canvasRef} width="1280" height="720" aria-label="WebGPU scene prototype" />
          <div className="scene-overlay">
            <span className="status-dot" aria-hidden="true" />
            <span>{gpuStatus}</span>
          </div>
        </section>
      </section>

      <footer>
        CLI, UI, and MCP consume the same revision-addressed project summary.
      </footer>
    </main>
  );
}

const root = document.getElementById("root");
if (!root) {
  throw new Error("workbench root element is missing");
}
createRoot(root).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
