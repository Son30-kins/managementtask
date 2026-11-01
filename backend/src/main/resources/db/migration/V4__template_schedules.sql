-- Lịch tự chạy của form mẫu: đến giờ thì tự điền form bằng giá trị đã lưu và tạo task.
create table template_schedules (
    id          uuid primary key,
    template_id uuid        not null unique references templates (id) on delete cascade,
    user_id     uuid        not null references users (id) on delete cascade,
    enabled     boolean     not null default true,
    recurrence  jsonb       not null,
    run_time    text        not null check (run_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'),
    form_values jsonb       not null default '{}',
    next_run_at timestamptz,
    last_run_at timestamptz,
    last_error  text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create index template_schedules_user_idx on template_schedules (user_id);
create index template_schedules_due_idx on template_schedules (next_run_at) where enabled;
