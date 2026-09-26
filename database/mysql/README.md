# MySQL database package

The Docker container runs `00-create-databases.sql` first, then the individual schema scripts and seed scripts. The six stateful applications are intentionally separated into different MySQL databases to make each service independently deployable.

Auth users are seeded in application code so passwords are always BCrypt-hashed. Hazard services have both SQL seeds and application fallback seeders.
