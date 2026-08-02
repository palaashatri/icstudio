# Security policy

ICStudio is pre-alpha software. Do not use it for production tapeout decisions, proprietary PDK processing, or untrusted remote MCP access.

## Reporting vulnerabilities

Report security issues privately to the repository owner through GitHub's private vulnerability reporting feature when enabled. Do not open a public issue for vulnerabilities involving arbitrary code execution, project corruption, secret exposure, PDK disclosure, sandbox escape, or remote MCP authorization bypass.

Include:

- affected commit and platform;
- minimal reproduction;
- expected and observed behaviour;
- impact assessment;
- whether the issue involves proprietary material.

## M0 security boundary

The M0 MCP implementation:

- supports local stdio only;
- exposes programme status only;
- offers no shell execution, filesystem browsing, network egress, design mutation, plugin loading, or solver dispatch;
- is a protocol smoke implementation, not a hardened general-purpose JSON-RPC server.

Remote transport, authentication, PDK handling, plugins, semantic design patches, and solver workers require dedicated threat models and acceptance gates before enablement.
