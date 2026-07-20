# Hex4j WebFlux

App template for Spring Webflux with example, including Hexagonal Architecture.

> See [HELP.md](./HELP.md) in Spanish

## Error Handling

Errors are handled at two layers that coexist: a centralized `GlobalErrorHandler`
(`transverse/exceptions`) for uncaught exceptions, and per-handler `handleError`
methods in `infrastructure/handlers` that catch errors inside the reactive flow.
See the *Manejo de Errores* section in [HELP.md](./HELP.md) for the full mapping
and why both are needed (including test isolation details).
