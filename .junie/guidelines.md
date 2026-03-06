* Java for all development, leveraging Spring Boot's ecosystem and mature tooling.
* Follow Spring Boot conventions. Default to immutability.
* Where possible, use lombok builders instead of setters or manual builders.
* Keep **class annotations minimal and purpose-driven**. Add only what's required **now**; no
  speculative "maybe later" annotations.
* For Spring components and similar, prefer **Lombok `@RequiredArgsConstructor`** for DI instead of
  explicit constructors.
* This is a green-field project. Design ahead, instead of worrying about legacy compat. Consolidate
  migrations into V1.
* **Note**: there is a bug in the interface with IntelliJ causing test suites to run very slowly and
  sequentially when using the IDE's built-in test runner. For faster execution of all tests, use
  `./gradlew test` instead.