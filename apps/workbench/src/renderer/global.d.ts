export {};

declare global {
  interface Window {
    icstudio: {
      readProjectSnapshot(): Promise<string>;
    };
  }
}
