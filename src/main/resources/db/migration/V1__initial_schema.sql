CREATE TABLE investment_plan (
    id            UUID PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    execution_day INTEGER NOT NULL CHECK (execution_day >= 1 AND execution_day <= 31),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE plan_investment (
    id         UUID PRIMARY KEY,
    plan_id    UUID NOT NULL REFERENCES investment_plan(id),
    instrument VARCHAR(50) NOT NULL,
    amount     NUMERIC(19, 4) NOT NULL CHECK (amount > 0)
);

CREATE INDEX idx_plan_investment_plan_id ON plan_investment(plan_id);

CREATE TABLE plan_execution (
    id             UUID PRIMARY KEY,
    plan_id        UUID NOT NULL REFERENCES investment_plan(id),
    execution_date DATE NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    result         VARCHAR(30),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version        BIGINT NOT NULL DEFAULT 0,
    UNIQUE (plan_id, execution_date)
);

CREATE INDEX idx_plan_execution_plan_id ON plan_execution(plan_id);
CREATE INDEX idx_plan_execution_stale ON plan_execution(status, created_at);

CREATE TABLE execution_order (
    id             UUID PRIMARY KEY,
    execution_id   UUID NOT NULL REFERENCES plan_execution(id),
    order_id       VARCHAR(255),
    instrument     VARCHAR(50) NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_execution_order_execution_id ON execution_order(execution_id);

CREATE TABLE processed_command (
    command_id   VARCHAR(255) PRIMARY KEY,
    command_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
