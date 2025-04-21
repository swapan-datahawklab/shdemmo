# Memory Bank Organization

## Directory Structure
```
/memory-bank/                  # Project-wide, permanent memory
├── README.md                  # This file
├── current_state.md          # SINGLE SOURCE OF TRUTH - Current system state
├── core/                     # Core project documentation
│   ├── projectbrief.md      # Project requirements and goals
│   ├── systemPatterns.md    # Architectural patterns
│   └── techContext.md       # Technical context
├── active/                   # Current state and progress
│   ├── activeContext.md     # Current focus and decisions
│   └── progress.md          # Project progress
├── product/                  # Product documentation
│   └── productContext.md    # Product context and goals
└── database/                # Database-specific memory
    ├── patterns.md          # Database patterns
    └── evolution.md         # Database changes history

/.cursor/memory-bank/         # IDE-specific, temporary memory
├── code_history.md          # Recent code changes
└── project_context.md       # Current development context
```

## Quick Access
```bash
# Get current system state
cat memory-bank/current_state.md

# Check project patterns
cat memory-bank/core/*

# View recent changes
cat .cursor/memory-bank/code_history.md
```

## Usage Guidelines

1. **Current State vs. History**
   - `current_state.md`: SINGLE SOURCE OF TRUTH for current system state
   - Other files: Historical context and evolution

2. **When to Use Each**
   - New to codebase? → `current_state.md`
   - Making changes? → `current_state.md` + relevant pattern files
   - Understanding history? → Evolution and history files
   - Recent changes? → `.cursor/memory-bank/`

3. **Update Frequency**
   - `current_state.md`: After EVERY change (CRITICAL)
   - `activeContext.md`: Major changes
   - `progress.md`: Task completion
   - `code_history.md`: Code changes
   - Pattern files: When patterns change

4. **Cross-References**
   - Always reference `current_state.md` first
   - Link to historical context when needed
   - Keep temporary and permanent memory in sync

## Best Practices

1. **Before Making Changes**
   - ALWAYS check `current_state.md` first
   - Review relevant patterns
   - Check recent changes

2. **After Making Changes**
   - Update `current_state.md` IMMEDIATELY
   - Update history files
   - Sync temporary memory

3. **Documentation Standards**
   - Use YAML for configuration
   - Use Mermaid for diagrams
   - Keep sections focused
   - Cross-reference when needed

4. **Memory Maintenance**
   - Keep `current_state.md` up to date
   - Archive completed work
   - Clean temporary memory
   - Sync documentation regularly

## Current State Updates

The `current_state.md` file MUST include:
1. Active Configuration
   - Container states
   - Access patterns
   - Schema details
   - Initialization checks

2. Current Patterns
   - Make targets
   - Connection patterns
   - Test structure
   - Initialization sequence

3. Active Context
   - Current issues
   - Recent changes
   - Next steps
   - Quick reference

4. Update Process
   ```bash
   # After any change
   vim memory-bank/current_state.md  # Update relevant sections
   git commit -m "Update current state: [WHAT CHANGED]"
   ``` 