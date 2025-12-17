# Contributing to EclipseLlama

Thank you for your interest in contributing to EclipseLlama! 🦙

## Code of Conduct

Be respectful, professional, and constructive in all interactions.

## How to Contribute

### Reporting Bugs
- Use GitHub Issues
- Include Eclipse version, Java version, Ollama version
- Provide steps to reproduce
- Include error logs if applicable

### Suggesting Features
- Open a GitHub Issue with the `enhancement` label
- Describe the use case and expected behavior
- Discuss before implementing large changes

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch** from `develop`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
   - Follow existing code style
   - Add comments for complex logic
   - Update documentation if needed
4. **Test thoroughly**
   - Test in Eclipse IDE (Run As → Eclipse Application)
   - Verify all existing features still work
5. **Commit with conventional commits**:
   ```
   feat: add new feature
   fix: resolve bug in X
   docs: update README
   refactor: improve code structure
   ```
6. **Push and create Pull Request** to `develop` branch

## Development Setup

### Prerequisites
- Eclipse IDE 2023-12 or newer
- Java 21+
- Ollama installed and running

### Building
1. Import project into Eclipse workspace
2. Ensure all dependencies are resolved
3. Run As → Eclipse Application to test

### Project Structure
```
eclipsellama/
├── src/                    # Plugin source code
├── test/                   # Unit tests
├── META-INF/               # Plugin manifest
├── plugin.xml              # Extension points
└── lib/                    # Dependencies
```

## Branch Strategy

- `main` - Stable releases only
- `develop` - Active development (default branch)
- `feature/*` - New features
- `fix/*` - Bug fixes

## Review Process

All PRs require:
- ✅ Code review by maintainer
- ✅ All tests passing
- ✅ No merge conflicts
- ✅ Conventional commit format

## Questions?

Open a GitHub Discussion or Issue!
