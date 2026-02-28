Here's an article about this application <article URL>

This is a description of the architectural patterns @docs/guides/prompts/architecture-elements.md

Do the following:

1. Read the article
2. Analyze the architecture to determine how the concepts in the article are implemented by the architecture elements
3. Create/Rewrite @docs/guides/article-part-N-code-guide.md - the guide should follow @docs/guides/prompts/guide-template.md
4. For each multi-service scenario in the Service Collaboration section, create a PlantUML sequence diagram in `docs/guides/diagrams/part-N/<scenario>.txt`
5. Verify that:
   * Every architectural element mentioned in the guide (services, modules, classes, interfaces, adapters, configuration files) exists in the codebase
   * Every link resolves to an existing file
   * Code excerpts match the actual source files. Standard editorial simplifications are acceptable (omitting try-catch wrappers, logging, replacing long parameters with `...`)
   * Sequence diagrams accurately reflect the actual call chains between services and classes in the codebase. Group participants by the runtime service process they execute in, not by the source module they are defined in (e.g., a shared library class that runs inside the Security System Service belongs in the Security System Service box)
6. Run `make` in `docs/guides/diagrams/` to generate the `.png` files from the PlantUML sources
