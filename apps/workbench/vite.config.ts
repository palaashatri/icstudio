import { defineConfig } from "vite";

export default defineConfig({
  root: ".",
  base: "./",
  esbuild: {
    jsx: "automatic"
  },
  build: {
    outDir: "dist-renderer",
    emptyOutDir: true,
    sourcemap: true,
    target: "es2022"
  }
});
