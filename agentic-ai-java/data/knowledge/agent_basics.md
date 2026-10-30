# Agent Basics

An AI agent is a controlled loop: observe, think, act, observe again.
The model chooses which tool to call; trusted application code executes it.
Every agent run has a step limit so it can never loop forever.

# Guardrails

Tools are allow-listed. Arguments are validated before a tool runs.
Each tool call has a timeout. Secrets live in environment variables, not source code.
Risky actions require human approval.
