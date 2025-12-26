#!/bin/bash
# Setup script for PostgreSQL primary node replication configuration

set -e

echo "Setting up replication configuration for primary node..."

# Configure pg_hba.conf for replication
cat >> "$PGDATA/pg_hba.conf" <<EOF

# Replication connections
host    replication     replicator      172.20.0.0/16           scram-sha-256
host    replication     replicator      0.0.0.0/0               scram-sha-256
EOF

echo "Replication configuration completed for primary node"
