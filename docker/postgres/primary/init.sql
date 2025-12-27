-- Database initialization script for Primary PostgreSQL node
-- This script sets up the initial database schema and configuration

-- Create database if not exists (already created by POSTGRES_DB env variable)
-- Database: ratings_reviews

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- For cryptographic functions and UUID generation

-- Create replication user for replica nodes
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'replicator') THEN
        CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator_password';
    END IF;
END
$$;

-- Grant necessary permissions
GRANT CONNECT ON DATABASE ratings_reviews TO replicator;

-- Create schema for the application
CREATE SCHEMA IF NOT EXISTS ratings_reviews;

-- Set default schema
SET search_path TO ratings_reviews, public;

--
---- Example table: users
--CREATE TABLE IF NOT EXISTS ratings_reviews.users (
--    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--    username VARCHAR(100) UNIQUE NOT NULL,
--    email VARCHAR(255) UNIQUE NOT NULL,
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--);

---- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION ratings_reviews.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE EXTENSION if not exists pg_trgm;

CREATE OR REPLACE FUNCTION ratings_reviews.update_application_rating_stats() RETURNS TRIGGER AS $$
BEGIN
  -- INSERT
  IF (TG_OP = 'INSERT') THEN
    INSERT INTO application_rating_stats (application_id, scale, count)
    VALUES (NEW.application_id, NEW.rating, 1)
    ON CONFLICT (application_id, scale) DO UPDATE SET count = application_rating_stats.count + 1;
    RETURN NEW;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully';
END
$$;
