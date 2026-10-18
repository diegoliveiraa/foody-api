CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

CREATE FUNCTION public.f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN public.unaccent('public.unaccent'::regdictionary, $1);

CREATE FUNCTION public.f_contains_pattern(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN '%' || replace(replace(replace(public.f_unaccent(lower($1)), '\', '\\'), '%', '\%'), '_', '\_') || '%';

CREATE INDEX users_name_unaccent_trgm_idx ON users USING gin (public.f_unaccent(lower(name)) public.gin_trgm_ops);

CREATE INDEX users_email_unaccent_trgm_idx ON users USING gin (public.f_unaccent(lower(email)) public.gin_trgm_ops);

CREATE INDEX products_name_unaccent_trgm_idx ON products USING gin (public.f_unaccent(lower(name)) public.gin_trgm_ops);
