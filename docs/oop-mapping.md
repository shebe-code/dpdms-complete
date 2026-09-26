# OOP defence map

| Course concept | Where it appears |
|---|---|
| Encapsulation | Private fields + getters/setters in `BaseIncident`, `HazardIncident`, `UserAccount`, `AlertLog`. |
| Abstraction | `HazardRules`, `AlertChannel`, `ReportGenerator` interfaces. |
| Inheritance | Every hazard entity extends `BaseIncident`. |
| Polymorphism | Factory-selected report generators and alert channel implementations. |
| Exceptions | `ForbiddenException`, `InvalidWorkflowException`, REST exception handlers. |
| DAO pattern | Spring Data repository interfaces. |
| Factory pattern | `AlertChannelFactory`, `ReportGeneratorFactory`. |
| Singleton pattern | `ReportTemplateRegistry.INSTANCE`. |
| Composition/has-a | Services contain repositories/rules/notifier clients as collaborators. |
| Class/object modelling | Domain entities, service classes and controller classes model the disaster office domain. |
