import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("icstudio", {
  readProjectSnapshot: (): Promise<string> => ipcRenderer.invoke("project:snapshot")
});
