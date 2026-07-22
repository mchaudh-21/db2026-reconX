# TICKET-ADV006 - ER Model (8 Entities)

```mermaid
erDiagram
    COUNTERPARTIES ||--o{ TRADES : "executes"
    INSTRUMENTS ||--o{ TRADES : "covers"
    TRADES ||--o{ SETTLEMENTS : "settles via"
    TRADES ||--o{ RECON\_BREAKS : "may produce"
    RECON\_JOBS ||--o{ RECON\_BREAKS : "detects"
    USERS ||--o{ RECON\_JOBS : "triggers"
    USERS ||--o{ AUDIT\_LOG : "actor reference - logical only, no FK"

    COUNTERPARTIES {
        bigint id PK
        varchar name
        char lei\_code UK
        varchar region
    }

    INSTRUMENTS {
        bigint id PK
        varchar symbol UK
        varchar name
        varchar asset\_class
        char currency
        char isin UK
        jsonb metadata "TICKET-ADV009"
    }

    USERS {
        bigint id PK
        varchar email UK
        varchar password\_hash
        varchar role
        boolean enabled
        timestamp created\_at
    }

    TRADES {
        bigint id PK
        varchar trade\_ref UK
        bigint instrument\_id FK
        bigint counterparty\_id FK
        varchar asset\_class
        varchar side
        numeric quantity
        numeric price
        date trade\_date "PARTITION KEY (TICKET-ADV007)"
        varchar status
        timestamp deleted\_at
        timestamp created\_at
        timestamp modified\_at
    }

    SETTLEMENTS {
        bigint id PK
        bigint trade\_id FK
        date settlement\_date
        numeric amount
        varchar status
    }

    RECON\_JOBS {
        bigint id PK
        varchar job\_id UK
        bigint triggered\_by\_user\_id FK
        date from\_date
        date to\_date
        varchar status
        timestamp started\_at
        timestamp finished\_at
        int trades\_processed
        int breaks\_detected
    }

    RECON\_BREAKS {
        bigint id PK
        bigint trade\_id FK
        bigint recon\_job\_id FK
        varchar discrepancy\_type
        varchar status
        timestamp detected\_at
        timestamp resolved\_at
        varchar resolution\_note
    }

    AUDIT\_LOG {
        bigint id PK
        varchar event\_id UK
        varchar trade\_ref
        varchar event\_type
        timestamp event\_timestamp
        varchar changed\_by "NO DATABASE FK - stores user email"
        jsonb before\_state
        jsonb after\_state
    }
```

