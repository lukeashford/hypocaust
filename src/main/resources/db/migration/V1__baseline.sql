create extension if not exists "uuid-ossp";

create table assistant
(
    id            uuid primary key,
    name          text  not null,
    system_prompt text,
    model         text  not null,
    params_json   jsonb not null
);

create table thread
(
    id               uuid primary key,
    title            text,
    created_at       timestamptz not null,
    last_activity_at timestamptz not null
);

create table message
(
    id               uuid primary key,
    thread_id        uuid        not null references thread (id) on delete cascade,
    author           text        not null check (author in ('USER', 'ASSISTANT', 'TOOL', 'SYSTEM')),
    created_at       timestamptz not null,
    content_json     jsonb       not null,
    attachments_json jsonb
);
create index idx_message_thread_time on message (thread_id, created_at);
create index idx_message_content_gin on message using gin (content_json);

create table run
(
    id           uuid primary key,
    thread_id    uuid not null references thread (id) on delete cascade,
    assistant_id uuid not null references assistant (id),
    status       text not null check (status in
                                      ('QUEUED', 'RUNNING', 'REQUIRES_ACTION', 'COMPLETED',
                                       'FAILED', 'CANCELLED')),
    kind         text not null check (kind in ('FULL', 'PARTIAL')),
    reason       text,
    started_at   timestamptz,
    completed_at timestamptz,
    usage_json   jsonb,
    error        text
);
create index idx_run_thread_started_at on run (thread_id, coalesce(started_at, completed_at));

create table artifact
(
    id            uuid primary key,
    thread_id     uuid        not null references thread (id) on delete cascade,
    run_id        uuid        references run (id) on delete set null,
    kind          text        not null check (kind in ('STRUCTURED_JSON', 'IMAGE', 'PDF', 'AUDIO',
                                                       'VIDEO')),
    stage         text        not null check (stage in ('PLAN', 'ANALYSIS', 'SCRIPT', 'IMAGES', 'DECK')),
    status        text        not null check (status in ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
    title         text,
    summary       text,
    mime          text,
    storage_key   text,
    inline_json   jsonb,
    meta_json     jsonb,
    created_at    timestamptz not null,
    supersedes_id uuid references artifact (id)
);
create index idx_artifact_thread_time on artifact (thread_id, created_at desc);
create index idx_artifact_stage on artifact (thread_id, stage, created_at desc);
create index idx_artifact_supersedes_chain on artifact (supersedes_id) where supersedes_id is not null;
create index idx_artifact_inline_gin on artifact using gin (inline_json);

create table event_log
(
    id          uuid primary key,
    thread_id   uuid        not null references thread (id) on delete cascade,
    run_id      uuid,
    message_id  uuid,
    event_type  text        not null check (event_type in
                                            ('run.created', 'run.updated', 'message.delta',
                                             'message.completed', 'artifact.created', 'error')),
    payload     jsonb       not null,
    occurred_at timestamptz not null default now(),
    dedupe_key  text
);
create index idx_event_thread_id on event_log (thread_id, id);
create index idx_event_dedupe on event_log (thread_id, dedupe_key) where dedupe_key is not null;