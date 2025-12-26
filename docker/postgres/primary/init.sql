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
--
---- Example table: ratings
--CREATE TABLE IF NOT EXISTS ratings_reviews.ratings (
--    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--    application_id UUID NOT NULL REFERENCES ratings_reviews.applications(id) ON DELETE CASCADE,
--    user_id UUID NOT NULL REFERENCES ratings_reviews.users(id) ON DELETE CASCADE,
--    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    UNIQUE(application_id, user_id)
--);
--
---- Example table: reviews
--CREATE TABLE IF NOT EXISTS ratings_reviews.reviews (
--    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--    application_id UUID NOT NULL REFERENCES ratings_reviews.applications(id) ON DELETE CASCADE,
--    user_id UUID NOT NULL REFERENCES ratings_reviews.users(id) ON DELETE CASCADE,
--    rating_id UUID REFERENCES ratings_reviews.ratings(id) ON DELETE CASCADE,
--    title VARCHAR(255),
--    comment TEXT,
--    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--);
--
---- Create indexes for performance
--CREATE INDEX IF NOT EXISTS idx_ratings_application_id ON ratings_reviews.ratings(application_id);
--CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings_reviews.ratings(user_id);
--CREATE INDEX IF NOT EXISTS idx_reviews_application_id ON ratings_reviews.reviews(application_id);
--CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON ratings_reviews.reviews(user_id);
--CREATE INDEX IF NOT EXISTS idx_reviews_rating_id ON ratings_reviews.reviews(rating_id);
--
---- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION ratings_reviews.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE EXTENSION if not exists pg_trgm;
--
---- Create triggers for automatic timestamp updates
--
--CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON ratings_reviews.users
--    FOR EACH ROW EXECUTE FUNCTION ratings_reviews.update_updated_at_column();
--
--CREATE TRIGGER update_ratings_updated_at BEFORE UPDATE ON ratings_reviews.ratings
--    FOR EACH ROW EXECUTE FUNCTION ratings_reviews.update_updated_at_column();
--
--CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON ratings_reviews.reviews
--    FOR EACH ROW EXECUTE FUNCTION ratings_reviews.update_updated_at_column();

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully';
END
$$;
