# Local Development Certificates

The certificates in this directory are used for local development with HTTPS. They are self-signed
and should not be used in production.

## Generating New Certificates

To generate a new self-signed certificate and key for local development, run the following command
from the project root:

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout infra/nginx/certs/localhost.key -out infra/nginx/certs/localhost.crt -subj "/C=US/ST=Local/L=Local/O=Local/OU=Local/CN=localhost"
```

## Security Note

The `.gitignore` file is configured to ignore `*.key` and `*.crt` files to prevent private keys from
being accidentally committed to the repository. Always ensure that your local certificates are not
tracked by git.
