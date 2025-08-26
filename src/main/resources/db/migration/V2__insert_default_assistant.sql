-- Insert default assistant record for the hardcoded assistant ID used by RunService
INSERT INTO assistant (id, name, system_prompt, model, params_json)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Default Assistant',
        'You are a helpful AI assistant that specializes in creating marketing content and brand analysis. You can analyze brands, create marketing pitches, generate scripts, and create visual content to support marketing campaigns.',
        'gpt-4',
        '{}'::jsonb);