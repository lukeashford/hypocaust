db-up:
	podman compose up -d postgres

db-down:
	podman compose down -v
