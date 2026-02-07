# Identity Modulith - ?°ì´?°ë² ?´ìŠ¤ ê°€?´ë“œ

> ?“… ìµœì¢… ?…ë°?´íŠ¸: 2026-02-05  
> ?—„ï¸?DB: PostgreSQL 18+  
> ? ï¸ ì£¼ìš” ë³€ê²? v3.0.0 - ?Œì´ë¸”ëª… ?œì??? DepartmentType Enum, ë¶€???íƒœ ê´€ë¦?

---

## ?“Š ?„ì²´ ?Œì´ë¸?êµ¬ì¡° (6ê°?

| ?Œì´ë¸”ëª… | ëª¨ë“ˆ | PK ?€??| ?¤ëª… |
|---------|------|---------|------|
| **org_departments** | Organization | VARCHAR(50) | ì¡°ì§(ë¶€?? ê³„ì¸µ êµ¬ì¡° |
| **agents** | User | VARCHAR(50) | ?¬ìš©???ë‹´?? ?•ë³´ |
| **rbac_roles** | RBAC | VARCHAR(50) | ??•  ?•ì˜ (POSITION, CHANNEL) |
| **rbac_permissions** | RBAC | VARCHAR(50) | ê¶Œí•œ ?•ì˜ |
| **rbac_role_permissions** | RBAC | Composite PK | ??• -ê¶Œí•œ ë§¤í•‘ (M:N) |
| **user_agent_roles** | RBAC | Composite PK | ?¬ìš©????•  ë§¤í•‘ (M:N) |

**ë³€ê²??¬í•­ (v3.0.0)**:
- ??`departmentEntities` ??`org_departments`
- ??`roles` ??`rbac_roles`
- ??`permissions` ??`rbac_permissions`
- ??`role_permissions` ??`rbac_role_permissions`
- ??`agent_roles` ??`user_agent_roles`
- ??PK ?€?? VARCHAR(36) ??VARCHAR(50) (UUID + ?¬ìœ  ê³µê°„)
- ??`org_departments`??`type` (DepartmentType Enum), `is_active` ì»¬ëŸ¼ ì¶”ê?

---

## ?¢ 1. org_departments (ì¡°ì§/ë¶€??

**ëª©ì **: ì¡°ì§ ê³„ì¸µ êµ¬ì¡° ê´€ë¦?(?¸ë¦¬ êµ¬ì¡° - Materialized Path ?¨í„´)

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **dept_id** | VARCHAR(50) | ??| ë¶€??ID (PK) | `dept-root-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?Œë„Œ??ID | `tenant-001` |
| name | VARCHAR(100) | ??| ë¶€?œëª… | `?¥ìŠ¤?„ë¡ `, `ê³ ê°?œë¹„?¤ë³¸ë¶€` |
| **type** | VARCHAR(20) | ??| ë¶€???€??(Enum) | `COMPANY`, `DIVISION`, `TEAM`, `GROUP`, `CUSTOM` |
| custom_type_name | VARCHAR(50) | ??| ?¬ìš©???•ì˜ ?€???´ë¦„ | `?°êµ¬??, `ì§€?? (type=CUSTOM???? |
| parent_dept_id | VARCHAR(50) | ??| ?ìœ„ ë¶€??ID (FK) | NULL=ìµœìƒ?? UUID=?˜ìœ„ |
| org_path | TEXT | ??| ì¡°ì§ ê²½ë¡œ (Materialized Path) | `/dept-root-001/dept-div-001/` |
| depth | INTEGER | ??| ?¸ë¦¬ ê¹Šì´ | 0(ìµœìƒ?? ~ 10 |
| display_order | INTEGER | ??| ?œì‹œ ?œì„œ | 1, 2, 3... |
| manager_id | VARCHAR(50) | ??| ë¶€?œì¥ ID | Agent ID |
| description | TEXT | ??| ë¶€???¤ëª… | `ê³ ê° ?œë¹„??ë°??ë‹´ ?…ë¬´ ì´ê´„` |
| **is_active** | BOOLEAN | ??| ?œì„±???íƒœ | TRUE (?œì„±), FALSE (ë¹„í™œ?? |
| created_at | TIMESTAMP | ??| ?ì„± ?¼ì‹œ | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?˜ì • ?¼ì‹œ | `2026-02-05 15:00:00` |
| created_by | VARCHAR(50) | ??| ?ì„±??ID | Agent ID |
| updated_by | VARCHAR(50) | ??| ?˜ì •??ID | Agent ID |

**?¸ë±??*: 
- `idx_dept_tenant`: `(tenant_id)`
- `idx_dept_parent`: `(parent_dept_id)`
- `idx_dept_org_path`: `(org_path)`
- `idx_dept_active`: `(is_active)`

**FK**: 
- `parent_dept_id` ??`org_departments(dept_id)` ON DELETE RESTRICT

**ì²´í¬ ?œì•½**:
- `chk_dept_type`: type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')
- `chk_custom_type`: type='CUSTOM'???Œë§Œ custom_type_name ?„ìˆ˜

**Department Type ?¤ëª…**:
| ?€??| ?¤ëª… | ?¬ìš© ?ˆì‹œ |
|------|------|----------|
| `COMPANY` | ìµœìƒ??ì¡°ì§ | ?Œì‚¬, ê³„ì—´??|
| `DIVISION` | ë³¸ë?ê¸?ì¡°ì§ | ê³ ê°?œë¹„?¤ë³¸ë¶€, ?ì—…ë³¸ë? |
| `TEAM` | ?€ê¸?ì¡°ì§ | ?¸ë°”?´ë“œ?€, ?„ì›ƒë°”ìš´?œí? |
| `GROUP` | ê·¸ë£¹/?ŒíŠ¸ | ê°œë°œê·¸ë£¹, ê¸°íš?ŒíŠ¸ |
| `CUSTOM` | ?¬ìš©???•ì˜ | custom_type_name?¼ë¡œ ?´ë¦„ ì§€??|

**?°ì´???ˆì‹œ**:
```sql
-- ìµœìƒ??ì¡°ì§ (COMPANY)
('dept-root-001', 'tenant-001', '?¥ìŠ¤?„ë¡ ', 'COMPANY', NULL, NULL, 
 '/dept-root-001/', 0, 1, NULL, '?¥ìŠ¤?„ë¡  ì£¼ì‹?Œì‚¬', TRUE, NOW(), NOW(), NULL, NULL)

-- ë³¸ë? (DIVISION)
('dept-div-001', 'tenant-001', 'ê³ ê°?œë¹„?¤ë³¸ë¶€', 'DIVISION', NULL, 'dept-root-001', 
 '/dept-root-001/dept-div-001/', 1, 1, NULL, 'ê³ ê° ?œë¹„??ì´ê´„', TRUE, NOW(), NOW(), NULL, NULL)

-- ?€ (TEAM)
('dept-team-001', 'tenant-001', '?¸ë°”?´ë“œ?€', 'TEAM', NULL, 'dept-div-001', 
 '/dept-root-001/dept-div-001/dept-team-001/', 2, 1, NULL, '?¸ë°”?´ë“œ ?„í™” ?ë‹´', TRUE, NOW(), NOW(), NULL, NULL)
```

---

## ?‘¤ 2. user_agents (?¬ìš©???ë‹´??


**ëª©ì **: ?œìŠ¤???¬ìš©???•ë³´ ê´€ë¦?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **agent_id** | VARCHAR(50) | ??| ?¬ìš©??ID (PK) | `agent-admin-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?Œë„Œ??ID | `tenant-001` |
| login_id | VARCHAR(50) | ??| ë¡œê·¸??ID (UK) | `admin`, `agent01` (?ë¬¸+?«ì, 4-20?? |
| password | VARCHAR(255) | ??| ë¹„ë?ë²ˆí˜¸ (BCrypt) | `$2a$10$...` (BCrypt ?´ì‹œ) |
| name | VARCHAR(100) | ??| ?¬ìš©?ëª… | `ê´€ë¦¬ì`, `?ê¸¸?? (2-50?? |
| employee_id | VARCHAR(50) | ??| ?¬ì› ë²ˆí˜¸ | `EMP001`, `2024001` |
| email | VARCHAR(100) | ??| ?´ë©”??| `admin@nexfron.com` |
| phone | VARCHAR(20) | ??| ?„í™”ë²ˆí˜¸ | `010-1234-5678` |
| dept_id | VARCHAR(50) | ??| ?Œì† ë¶€??ID (FK) | org_departments(dept_id) |
| status | VARCHAR(20) | ??| ?íƒœ | **`ACTIVE`** (?œì„±), **`SUSPENDED`** (?•ì?), **`RETIRED`** (?´ì‚¬) |
| password_must_change | BOOLEAN | ??| ë¹„ë?ë²ˆí˜¸ ë³€ê²??„ìš” | `FALSE` (ê¸°ë³¸ê°? |
| created_at | TIMESTAMP | ??| ?ì„± ?¼ì‹œ | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?˜ì • ?¼ì‹œ | `2026-02-05 15:00:00` |
| suspended_at | TIMESTAMP | ??| ?•ì? ?¼ì‹œ | `2025-12-31 23:59:59` |
| retired_at | TIMESTAMP | ??| ?´ì‚¬ ?¼ì‹œ | `2025-12-31 23:59:59` |
| scheduled_delete_at | TIMESTAMP | ??| ?? œ ?ˆì • ?¼ì‹œ | ?´ì‚¬ ??90??|
| created_by | VARCHAR(50) | ??| ?ì„±??ID | Agent ID |
| updated_by | VARCHAR(50) | ??| ?˜ì •??ID | Agent ID |
| suspended_by | VARCHAR(50) | ??| ?•ì? ì²˜ë¦¬??ID | Agent ID |
| retired_by | VARCHAR(50) | ??| ?´ì‚¬ ì²˜ë¦¬??ID | Agent ID |
| version | BIGINT | ??| ?™ê???? ê¸ˆ ë²„ì „ | 0 (ê¸°ë³¸ê°? |

**?¸ë±??*: 
- UK: `(tenant_id, login_id)` (ë³µí•© ? ë‹ˆ??
- `idx_agent_tenant`: `(tenant_id)`
- `idx_agent_login`: `(login_id)`
- `idx_agent_dept`: `(dept_id)`
- `idx_agent_status`: `(status)`
- `idx_agent_scheduled_delete`: `(scheduled_delete_at)` WHERE scheduled_delete_at IS NOT NULL

**FK**: 
- `dept_id` ??`org_departments(dept_id)` ON DELETE SET NULL

**ì²´í¬ ?œì•½**:
- `chk_agent_status`: status IN ('ACTIVE', 'SUSPENDED', 'RETIRED')

**?°ì´???ˆì‹œ**:
```sql
-- ê´€ë¦¬ì (ë¹„ë?ë²ˆí˜¸: password123)
('agent-admin-001', 'tenant-001', 'admin', 
 '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', 
 'ê´€ë¦¬ì', 'EMP001', 'admin@nexfron.com', '010-1234-5678', 
 'dept-root-001', 'ACTIVE', FALSE, NOW(), NOW(), NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, 0)

-- ?€??
('agent-lead-001', 'tenant-001', 'teamlead01', 
 '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', 
 'ê¹€?€??, 'EMP002', 'teamlead@nexfron.com', '010-2345-6789', 
 'dept-div-001', 'ACTIVE', FALSE, NOW(), NOW(), NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, 0)
```

**ë¹„ë?ë²ˆí˜¸ ?´ì‹œ**:
- **?Œê³ ë¦¬ì¦˜**: BCrypt (Spring Security ê¸°ë³¸)
- **ê°•ë„**: 10 rounds
- **?ŒìŠ¤??ë¹„ë?ë²ˆí˜¸**: `password123`
- **?´ì‹œ ê°?*: `$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy`

---

## ?­ 3. rbac_roles (??• )

**ëª©ì **: ??•  ?•ì˜ ë°?ê´€ë¦?(POSITION, CHANNEL ?€??

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(50) | ??| ??•  ID (PK) | `role-admin-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?Œë„Œ??ID | `tenant-001` |
| name | VARCHAR(50) | ??| ??• ëª?(UK) | **`ADMIN`**, **`TEAM_LEAD`**, **`AGENT`** (ì§ê¸‰) <br> **`INBOUND_AGENT`**, **`CHAT_AGENT`** (ì±„ë„) |
| **type** | VARCHAR(20) | ??| ??•  ?€??| **`POSITION`** (ì§ê¸‰ ê¸°ë°˜), **`CHANNEL`** (ì±„ë„ ê¸°ë°˜) |
| **data_scope** | VARCHAR(20) | ??| ?°ì´???¤ì½”???ˆë²¨ | **`ADMIN`**, **`TEAM_LEAD`**, **`MEMBER`** (POSITION???Œë§Œ) |
| description | VARCHAR(255) | ??| ??•  ?¤ëª… | `?œìŠ¤???„ì²´ ê´€ë¦¬ì - ëª¨ë“  ê¶Œí•œ ë³´ìœ ` |
| is_active | BOOLEAN | ??| ?œì„±???íƒœ | TRUE (ê¸°ë³¸ê°? |
| created_at | TIMESTAMP | ??| ?ì„± ?¼ì‹œ | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?˜ì • ?¼ì‹œ | `2026-02-05 15:00:00` |

**?¸ë±??*: 
- UK: `(tenant_id, name)` (ë³µí•© ? ë‹ˆ??
- `idx_role_tenant`: `(tenant_id)`
- `idx_role_type`: `(type)`
- `idx_role_active`: `(is_active)`

**ì²´í¬ ?œì•½**:
- `chk_role_type`: type IN ('POSITION', 'CHANNEL')
- `chk_role_data_scope`: 
  - POSITION???? data_scope IN ('ADMIN', 'TEAM_LEAD', 'MEMBER')
  - CHANNEL???? data_scope IS NULL

**??•  ?€???¤ëª…**:
| ?€??| ?¤ëª… | ?ˆì‹œ | data_scope |
|------|------|------|------------|
| `POSITION` | ì§ê¸‰ ê¸°ë°˜ ??•  | ADMIN, TEAM_LEAD, AGENT | ?„ìˆ˜ (ADMIN, TEAM_LEAD, MEMBER) |
| `CHANNEL` | ì±„ë„ ê¸°ë°˜ ??•  | INBOUND_AGENT, CHAT_AGENT | NULL |

**?°ì´???¤ì½”???ˆë²¨**:
| ?ˆë²¨ | ?¤ëª… | ì¡°íšŒ ë²”ìœ„ |
|------|------|----------|
| `ADMIN` | ?„ì²´ ?°ì´???‘ê·¼ | ?Œë„Œ????ëª¨ë“  ë¶€??|
| `TEAM_LEAD` | ë³¸ì¸ ë¶€??+ ?˜ìœ„ | ë³¸ì¸ ë¶€?œì? ?˜ìœ„ ë¶€???„ì²´ |
| `MEMBER` | ë³¸ì¸ ë¶€?œë§Œ | ë³¸ì¸???Œì†??ë¶€?œë§Œ |

**?°ì´???ˆì‹œ**:
```sql
-- POSITION ??• 
('role-admin-001', 'tenant-001', 'ADMIN', 'POSITION', 'ADMIN', 
 '?œìŠ¤??ê´€ë¦¬ì (?„ì²´ ì¡°ì§ ?‘ê·¼)', TRUE, NOW(), NOW()),
('role-teamlead-001', 'tenant-001', 'TEAM_LEAD', 'POSITION', 'TEAM_LEAD', 
 '?€??(ë³¸ì¸ ?€ + ?˜ìœ„ ë¶€???‘ê·¼)', TRUE, NOW(), NOW()),
('role-agent-001', 'tenant-001', 'AGENT', 'POSITION', 'MEMBER', 
 '?¼ë°˜ ?ë‹´??(ë³¸ì¸ ?€ë§??‘ê·¼)', TRUE, NOW(), NOW())

-- CHANNEL ??• 
('role-ch-inbound', 'tenant-001', 'INBOUND_AGENT', 'CHANNEL', NULL, 
 '?¸ë°”?´ë“œ ?„í™” ?ë‹´', TRUE, NOW(), NOW()),
('role-ch-chat', 'tenant-001', 'CHAT_AGENT', 'CHANNEL', NULL, 
 'ì±„íŒ… ?ë‹´', TRUE, NOW(), NOW())
```

---

## ?”‘ 4. rbac_permissions (ê¶Œí•œ)

**ëª©ì **: ê¶Œí•œ ?•ì˜ ë°?ê´€ë¦?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **permission_id** | VARCHAR(50) | ??| ê¶Œí•œ ID (PK) | `perm-agent-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?Œë„Œ??ID | `tenant-001` |
| code | VARCHAR(100) | ??| ê¶Œí•œ ì½”ë“œ (UK) | `agent:create`, `dept:read`, `role:manage` |
| name | VARCHAR(100) | ??| ê¶Œí•œ ?´ë¦„ | `?ë‹´???ì„±`, `ë¶€??ì¡°íšŒ`, `??•  ê´€ë¦? |
| description | VARCHAR(255) | ??| ê¶Œí•œ ?¤ëª… | `?ˆë¡œ???ë‹´??ê³„ì • ?ì„±` |
| category | VARCHAR(50) | ??| ê¶Œí•œ ì¹´í…Œê³ ë¦¬ | `AGENT`, `DEPARTMENT`, `RBAC`, `CHANNEL` |
| created_at | TIMESTAMP | ??| ?ì„± ?¼ì‹œ | `2026-01-21 10:00:00` |

**?¸ë±??*: 
- UK: `(tenant_id, code)` (ë³µí•© ? ë‹ˆ??
- `idx_permission_tenant`: `(tenant_id)`
- `idx_permission_category`: `(category)`

**ê¶Œí•œ ì½”ë“œ ?•ì‹**: `?„ë©”???¡ì…˜`
- ?„ë©”?? agent, dept, role, permission, channel
- ?¡ì…˜: create, read, update, delete, suspend, activate, transfer ??

**ê¶Œí•œ ì¹´í…Œê³ ë¦¬**:
| ì¹´í…Œê³ ë¦¬ | ?¤ëª… | ê¶Œí•œ ?ˆì‹œ |
|----------|------|----------|
| `AGENT` | ?ë‹´??ê´€ë¦?| agent:create, agent:read, agent:update, agent:delete |
| `DEPARTMENT` | ë¶€??ê´€ë¦?| dept:create, dept:read, dept:update, dept:delete, dept:move |
| `RBAC` | ??• /ê¶Œí•œ ê´€ë¦?| role:create, role:delete, permission:assign |
| `CHANNEL` | ì±„ë„ë³?ê¶Œí•œ | channel:inbound:receive, channel:chat:message |

**?°ì´???ˆì‹œ**:
```sql
-- AGENT ì¹´í…Œê³ ë¦¬
('perm-agent-001', 'tenant-001', 'agent:create', '?ë‹´???ì„±', 
 '?ˆë¡œ???ë‹´??ê³„ì • ?ì„±', 'AGENT', NOW()),
('perm-agent-002', 'tenant-001', 'agent:read', '?ë‹´??ì¡°íšŒ', 
 '?ë‹´???•ë³´ ì¡°íšŒ', 'AGENT', NOW()),

-- DEPARTMENT ì¹´í…Œê³ ë¦¬
('perm-dept-001', 'tenant-001', 'dept:create', 'ë¶€???ì„±', 
 '?ˆë¡œ??ë¶€???ì„±', 'DEPARTMENT', NOW()),
('perm-dept-002', 'tenant-001', 'dept:read', 'ë¶€??ì¡°íšŒ', 
 'ë¶€???•ë³´ ì¡°íšŒ', 'DEPARTMENT', NOW()),

-- CHANNEL ì¹´í…Œê³ ë¦¬
('perm-ch-in-001', 'tenant-001', 'channel:inbound:receive', '?¸ë°”?´ë“œ ?˜ì‹ ', 
 '?¸ë°”?´ë“œ ?„í™” ?˜ì‹ ', 'CHANNEL', NOW()),
('perm-ch-chat-001', 'tenant-001', 'channel:chat:message', 'ì±„íŒ… ë©”ì‹œì§€', 
 'ì±„íŒ… ë©”ì‹œì§€ ?¡ìˆ˜??, 'CHANNEL', NOW())
```

---

**ëª©ì **: ??•  ?•ì˜ ë°?ê´€ë¦?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(36) | ??| ??•  ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ??| ?Œë„Œ??ID | `tenant-001` |
| name | VARCHAR(64) | ??| ??• ëª?(UK) | **`ADMIN`**, **`MANAGER`**, **`TEAM_LEAD`**, **`MEMBER`** (ì§ì±…) <br> **`PHONE_AGENT`**, **`CHAT_AGENT`**, **`EMAIL_AGENT`** (ì±„ë„) |
| type | VARCHAR(32) | ??| ??•  ?€??| **`POSITION`** (ì§ì±… ê¸°ë°˜), **`CHANNEL`** (ì±„ë„ ê¸°ë°˜), **`SKILL`** (?¤í‚¬ ê¸°ë°˜) |
| description | VARCHAR(255) | ??| ??•  ?¤ëª… | `?œìŠ¤???„ì²´ ê´€ë¦¬ì - ëª¨ë“  ê¶Œí•œ ë³´ìœ ` |
| is_active | BOOLEAN | ??| ?œì„±???íƒœ | **`true`** (?œì„±), **`false`** (ë¹„í™œ???¼ë¦¬ ?? œ) |
| version | BIGINT | ??| ?™ê???? ê¸ˆ ë²„ì „ | 0, 1, 2... (?™ì‹œ???œì–´?? |
| created_at | TIMESTAMP | ??| ?ì„± ?¼ì‹œ | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?˜ì • ?¼ì‹œ | `2026-01-21 15:00:00` |

**?¸ë±??*: `(tenant_id, name)` UK, `tenant_id`, `is_active`

**?œì? ??•  (8ê°?**:

### ì§ì±… ê¸°ë°˜ (POSITION)
1. **ADMIN** - ?œìŠ¤???„ì²´ ê´€ë¦¬ì (35ê°??„ì²´ ê¶Œí•œ)
2. **MANAGER** - ë¶€??ê´€ë¦¬ì (12ê°?ê¶Œí•œ)
3. **TEAM_LEAD** - ?€ ë¦¬ë” (5ê°?ê¶Œí•œ)
4. **MEMBER** - ?¼ë°˜ ?¬ìš©??(4ê°?ê¶Œí•œ)

### ì±„ë„ ê¸°ë°˜ (CHANNEL)
5. **PHONE_AGENT** - ?„í™” ?ë‹´??(3ê°?ê¶Œí•œ)
6. **CHAT_AGENT** - ì±„íŒ… ?ë‹´??(2ê°?ê¶Œí•œ)
7. **EMAIL_AGENT** - ?´ë©”???ë‹´??(1ê°?ê¶Œí•œ)
8. **SUPERVISOR** - ?ˆí¼ë°”ì´?€ (??ê´€ë¦?

**?°ì´???ˆì‹œ**:
```sql
('660e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'ADMIN', 'POSITION', 
 '?œìŠ¤???„ì²´ ê´€ë¦¬ì - ëª¨ë“  ê¶Œí•œ ë³´ìœ ', true, 0, NOW(), NOW())
```

---

## ?”‘ 4. permissions (ê¶Œí•œ)

**ëª©ì **: ?¸ë¶„?”ëœ ê¶Œí•œ ?•ì˜

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **permission_id** | VARCHAR(36) | ??| ê¶Œí•œ ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ??| ?Œë„Œ??ID | `tenant-001` |
| code | VARCHAR(128) | ??| ê¶Œí•œ ì½”ë“œ (UK) | **`domain:action`** ?•ì‹ |
| created_at | TIMESTAMP | ??| ?ì„± ?¼ì‹œ | `2026-01-21 10:00:00` |

**?¸ë±??*: `(tenant_id, code)` UK, `tenant_id`

**?œì? ê¶Œí•œ ì½”ë“œ (35ê°?**:

### ?¬ìš©??ê´€ë¦?(user)
- `user:create` - ?¬ìš©???ì„±
- `user:read` - ?¬ìš©??ì¡°íšŒ (?„ì²´)
- `user:read:self` - ë³¸ì¸ ?•ë³´ ì¡°íšŒ
- `user:update` - ?¬ìš©???˜ì •
- `user:update:self` - ë³¸ì¸ ?•ë³´ ?˜ì •
- `user:delete` - ?¬ìš©???? œ
- `user:manage` - ?¬ìš©???„ì²´ ê´€ë¦?
- `user:assign:role` - ??•  ? ë‹¹
- `user:reset:password` - ë¹„ë?ë²ˆí˜¸ ì´ˆê¸°??

### ì¡°ì§ ê´€ë¦?(org)
- `org:view` - ì¡°ì§??ì¡°íšŒ
- `org:create` - ë¶€???ì„±
- `org:update` - ë¶€???˜ì •
- `org:move` - ë¶€???´ë™
- `org:delete` - ë¶€???? œ
- `org:manage` - ì¡°ì§ ?„ì²´ ê´€ë¦?

### RBAC ê´€ë¦?(rbac)
- `rbac:view` - ??• /ê¶Œí•œ ì¡°íšŒ
- `rbac:create:role` - ??•  ?ì„±
- `rbac:update:role` - ??•  ?˜ì •
- `rbac:delete:role` - ??•  ?? œ
- `rbac:create:permission` - ê¶Œí•œ ?ì„±
- `rbac:update:permission` - ê¶Œí•œ ?˜ì •
- `rbac:delete:permission` - ê¶Œí•œ ?? œ
- `rbac:assign:permission` - ê¶Œí•œ ? ë‹¹
- `rbac:configure` - RBAC ?„ì²´ ?¤ì •

### ë³´ê³ ??(report)
- `report:view` - ë³´ê³ ??ì¡°íšŒ
- `report:read` - ë³´ê³ ???½ê¸°
- `report:export` - ë³´ê³ ???´ë³´?´ê¸°
- `report:manage` - ë³´ê³ ??ê´€ë¦?

### ì±„ë„ (phone, chat, email)
- `phone:accept` - ?„í™” ?˜ì‹ 
- `phone:hold` - ?„í™” ë³´ë¥˜
- `phone:transfer` - ?„í™” ?„í™˜
- `chat:send` - ì±„íŒ… ?„ì†¡
- `chat:read` - ì±„íŒ… ?½ê¸°
- `email:send` - ?´ë©”???„ì†¡
- `queue:manage` - ??ê´€ë¦?

**?°ì´???ˆì‹œ**:
```sql
('550e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'user:create', NOW())
```

---

## ?”— 5. rbac_role_permissions (??• -ê¶Œí•œ ë§¤í•‘)

**ëª©ì **: ??• ê³?ê¶Œí•œ???¤ë???ê´€ê³?(M:N)

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(50) | ??| ??•  ID (PK, FK) | `role-admin-001` |
| **permission_id** | VARCHAR(50) | ??| ê¶Œí•œ ID (PK, FK) | `perm-agent-001` |
| assigned_at | TIMESTAMP | ??| ? ë‹¹ ?¼ì‹œ | `2026-01-21 10:00:00` |
| assigned_by | VARCHAR(50) | ??| ? ë‹¹??ID | Agent ID |

**PK**: `(role_id, permission_id)` (ë³µí•© PK)

**?¸ë±??*: 
- `idx_rp_role`: `(role_id)`
- `idx_rp_permission`: `(permission_id)`

**FK**: 
- `role_id` ??`rbac_roles(role_id)` ON DELETE CASCADE
- `permission_id` ??`rbac_permissions(permission_id)` ON DELETE CASCADE

**ì´ˆê¸° ë§¤í•‘ ??*:
- **ADMIN**: 35ê°?ê¶Œí•œ (?„ì²´ ê¶Œí•œ)
- **TEAM_LEAD**: 6ê°?ê¶Œí•œ (agent:read, agent:update, agent:transfer, dept:read, role:read, permission:read)
- **AGENT**: 3ê°?ê¶Œí•œ (agent:read, dept:read, role:read)
- **INBOUND_AGENT**: 3ê°?ê¶Œí•œ (channel:inbound:receive, channel:inbound:hold, channel:inbound:transfer)
- **CHAT_AGENT**: 3ê°?ê¶Œí•œ (channel:chat:message, channel:chat:file, channel:chat:emoji)
- **MULTI_CHANNEL_AGENT**: 14ê°?ê¶Œí•œ (ëª¨ë“  ì±„ë„ ê¶Œí•œ)

**?°ì´???ˆì‹œ**:
```sql
-- ADMIN ??• ??ëª¨ë“  ê¶Œí•œ ? ë‹¹
('role-admin-001', 'perm-agent-001', NOW(), NULL),
('role-admin-001', 'perm-agent-002', NOW(), NULL),
('role-admin-001', 'perm-dept-001', NOW(), NULL),
...

-- TEAM_LEAD ??• ???¼ë? ê¶Œí•œ ? ë‹¹
('role-teamlead-001', 'perm-agent-002', NOW(), NULL),  -- agent:read
('role-teamlead-001', 'perm-agent-003', NOW(), NULL),  -- agent:update
('role-teamlead-001', 'perm-dept-002', NOW(), NULL),   -- dept:read
...
```

---

## ?‘¥ 6. user_agent_roles (?¬ìš©????•  ë§¤í•‘)

**ëª©ì **: ?¬ìš©?ì? ??• ???¤ë???ê´€ê³?(M:N) - ?˜ë‚˜???¬ìš©?ì—ê²??¬ëŸ¬ ??•  ? ë‹¹ ê°€??

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **agent_id** | VARCHAR(50) | ??| ?¬ìš©??ID (PK, FK) | `agent-admin-001` |
| **role_id** | VARCHAR(50) | ??| ??•  ID (PK, FK) | `role-admin-001` |
| assigned_at | TIMESTAMP | ??| ? ë‹¹ ?¼ì‹œ | `2026-01-21 10:00:00` |
| assigned_by | VARCHAR(50) | ??| ? ë‹¹??ID | Agent ID |

**PK**: `(agent_id, role_id)` (ë³µí•© PK)

**?¸ë±??*: 
- `idx_ar_agent`: `(agent_id)`
- `idx_ar_role`: `(role_id)`

**FK**: 
- `agent_id` ??`agents(agent_id)` ON DELETE CASCADE
- `role_id` ??`rbac_roles(role_id)` ON DELETE CASCADE

**?¬ìš© ?ˆì‹œ**:
???¬ìš©?ê? ?¬ëŸ¬ ??• ???™ì‹œ??ê°€ì§????ˆìŠµ?ˆë‹¤:
- POSITION ??•  1ê°?+ CHANNEL ??•  Nê°?
- ?? `TEAM_LEAD` (ì§ê¸‰) + `INBOUND_AGENT` (ì±„ë„) + `CHAT_AGENT` (ì±„ë„)

**?°ì´???ˆì‹œ**:
```sql
-- ê´€ë¦¬ì: ADMIN ??• ë§?
('agent-admin-001', 'role-admin-001', NOW(), NULL),

-- ?€?? TEAM_LEAD + INBOUND_AGENT
('agent-lead-001', 'role-teamlead-001', NOW(), NULL),
('agent-lead-001', 'role-ch-inbound', NOW(), NULL),

-- ?¼ë°˜ ?ë‹´?? AGENT + INBOUND_AGENT + CHAT_AGENT (ë©€??ì±„ë„)
('agent-001', 'role-agent-001', NOW(), NULL),
('agent-001', 'role-ch-inbound', NOW(), NULL),
('agent-001', 'role-ch-chat', NOW(), NULL)
```

---

## ?“Š ì´ˆê¸° ?°ì´???”ì•½

### ??•  (8ê°?
| ??• ëª?| ?€??| ?°ì´???¤ì½”??| ?¤ëª… |
|--------|------|---------------|------|
| ADMIN | POSITION | ADMIN | ?œìŠ¤??ê´€ë¦¬ì (?„ì²´ ê¶Œí•œ) |
| TEAM_LEAD | POSITION | TEAM_LEAD | ?€??(?€ + ?˜ìœ„ ?‘ê·¼) |
| AGENT | POSITION | MEMBER | ?¼ë°˜ ?ë‹´??(ë³¸ì¸ ?€ë§? |
| INBOUND_AGENT | CHANNEL | NULL | ?¸ë°”?´ë“œ ?„í™” ?ë‹´ |
| OUTBOUND_AGENT | CHANNEL | NULL | ?„ì›ƒë°”ìš´???„í™” ?ë‹´ |
| CHAT_AGENT | CHANNEL | NULL | ì±„íŒ… ?ë‹´ |
| EMAIL_AGENT | CHANNEL | NULL | ?´ë©”???ë‹´ |
| MULTI_CHANNEL_AGENT | CHANNEL | NULL | ë©€?°ì±„???ë‹´ (ëª¨ë“  ì±„ë„) |

### ê¶Œí•œ (31ê°?
| ì¹´í…Œê³ ë¦¬ | ê¶Œí•œ ??| ?ˆì‹œ |
|----------|---------|------|
| AGENT | 9ê°?| agent:create, agent:read, agent:update, agent:delete, agent:suspend, agent:activate, agent:transfer, agent:role:assign, agent:password:reset |
| DEPARTMENT | 6ê°?| dept:create, dept:read, dept:update, dept:delete, dept:move, dept:deactivate |
| RBAC | 6ê°?| role:create, role:read, role:update, role:delete, permission:read, permission:assign |
| CHANNEL | 10ê°?| channel:inbound:receive/hold/transfer (3), channel:outbound:call/campaign (2), channel:chat:message/file/emoji (3), channel:email:send/receive (2) |

### ?˜í”Œ ?°ì´??
**ë¶€??(4ê°?**:
```
?¥ìŠ¤?„ë¡  (COMPANY)
?”â??€ ê³ ê°?œë¹„?¤ë³¸ë¶€ (DIVISION)
    ?œâ??€ ?¸ë°”?´ë“œ?€ (TEAM)
    ?”â??€ ?„ì›ƒë°”ìš´?œí? (TEAM)
```

**?¬ìš©??(3ê°?**:
| ë¡œê·¸??ID | ?´ë¦„ | ë¶€??| ??•  | ë¹„ë?ë²ˆí˜¸ |
|-----------|------|------|------|----------|
| admin | ê´€ë¦¬ì | ?¥ìŠ¤?„ë¡  | ADMIN | password123 |
| teamlead01 | ê¹€?€??| ê³ ê°?œë¹„?¤ë³¸ë¶€ | TEAM_LEAD, INBOUND_AGENT | password123 |
| agent01 | ?ê¸¸??| ?¸ë°”?´ë“œ?€ | AGENT, INBOUND_AGENT, CHAT_AGENT | password123 |

---

## ?” ì£¼ìš” ì¿¼ë¦¬ ?ˆì‹œ

### 1. ?¬ìš©?ì˜ ëª¨ë“  ê¶Œí•œ ì¡°íšŒ (ê³„ì‚°??ê¶Œí•œ)
```sql
SELECT DISTINCT p.code, p.name, p.category
FROM user_agent_roles ar
JOIN rbac_role_permissions rp ON ar.role_id = rp.role_id
JOIN rbac_permissions p ON rp.permission_id = p.permission_id
WHERE ar.agent_id = 'agent-admin-001'
  AND ar.tenant_id = 'tenant-001'
ORDER BY p.category, p.code;
```

### 2. ??• ë³?ê¶Œí•œ ???•ì¸
```sql
SELECT r.name AS role_name, r.type, COUNT(rp.permission_id) AS permission_count
FROM rbac_roles r
LEFT JOIN rbac_role_permissions rp ON r.role_id = rp.role_id
WHERE r.tenant_id = 'tenant-001'
GROUP BY r.role_id, r.name, r.type
ORDER BY r.type, r.name;
```

### 3. ë¶€?œë³„ ?¬ìš©????(?œì„± ?¬ìš©?ë§Œ)
```sql
SELECT d.name AS dept_name, COUNT(a.agent_id) AS agent_count
FROM org_departments d
LEFT JOIN agents a ON d.dept_id = a.dept_id AND a.status = 'ACTIVE'
WHERE d.tenant_id = 'tenant-001' AND d.is_active = TRUE
GROUP BY d.dept_id, d.name
ORDER BY d.org_path;
```

### 4. ?˜ìœ„ ë¶€??ì¡°íšŒ (Materialized Path ?œìš©)
```sql
SELECT dept_id, name, type, depth, org_path
FROM org_departments
WHERE tenant_id = 'tenant-001'
  AND org_path LIKE '/dept-root-001/%'
ORDER BY org_path;
```

### 5. ?¹ì • ê¶Œí•œ??ê°€ì§??¬ìš©??ì°¾ê¸°
```sql
SELECT DISTINCT a.login_id, a.name, a.email
FROM agents a
JOIN user_agent_roles ar ON a.agent_id = ar.agent_id
JOIN rbac_role_permissions rp ON ar.role_id = rp.role_id
JOIN rbac_permissions p ON rp.permission_id = p.permission_id
WHERE p.code = 'agent:delete'
  AND a.tenant_id = 'tenant-001'
  AND a.status = 'ACTIVE'
ORDER BY a.name;
```

---

## ?? ?±ëŠ¥ ìµœì ??

### ?¸ë±???„ëµ
1. **ë³µí•© ? ë‹ˆ???¸ë±??*: ?Œë„Œ??ê²©ë¦¬ ë°?ì¤‘ë³µ ë°©ì?
   - `(tenant_id, login_id)` - agents
   - `(tenant_id, name)` - rbac_roles
   - `(tenant_id, code)` - rbac_permissions

2. **ì¡°íšŒ ?±ëŠ¥ ?¸ë±??*:
   - `org_path` - ?˜ìœ„ ë¶€??ì¡°íšŒ ìµœì ??
   - `status` - ?œì„± ?¬ìš©???„í„°ë§?
   - `type`, `is_active` - ??• /ë¶€???€?…ë³„ ì¡°íšŒ

3. **FK ?¸ë±??*: JOIN ?±ëŠ¥ ?¥ìƒ
   - `parent_dept_id`, `dept_id`, `role_id`, `permission_id`

### ì¿¼ë¦¬ ìµœì ????
1. **Materialized Path**: `LIKE '/parent/%'`ë¡??˜ìœ„ ë¶€??ë¹ ë¥´ê²?ì¡°íšŒ
2. **ë³µí•© PK**: ë§¤í•‘ ?Œì´ë¸”ì—??ì¤‘ë³µ ë°©ì? ë°?ë¹ ë¥¸ ì¡°íšŒ
3. **ON DELETE CASCADE**: ??• /ê¶Œí•œ ?? œ ??ë§¤í•‘ ?ë™ ?•ë¦¬
4. **?™ê???? ê¸ˆ**: `version` ì»¬ëŸ¼?¼ë¡œ ?™ì‹œ???œì–´

---

## ?“ ë§ˆì´ê·¸ë ˆ?´ì…˜ ê°€?´ë“œ

### v2.x ??v3.0.0 ë§ˆì´ê·¸ë ˆ?´ì…˜

**1. ?Œì´ë¸”ëª… ë³€ê²?*:
```sql
ALTER TABLE departmentEntities RENAME TO org_departments;
ALTER TABLE roles RENAME TO rbac_roles;
ALTER TABLE permissions RENAME TO rbac_permissions;
ALTER TABLE role_permissions RENAME TO rbac_role_permissions;
ALTER TABLE agent_roles RENAME TO user_agent_roles;
```

**2. ë¶€???€??ë°??íƒœ ì»¬ëŸ¼ ì¶”ê?**:
```sql
ALTER TABLE org_departments ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'TEAM';
ALTER TABLE org_departments ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE org_departments ADD CONSTRAINT chk_dept_type 
  CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM'));
```

**3. ??•  ?Œì´ë¸”ì— data_scope ì¶”ê?**:
```sql
ALTER TABLE rbac_roles ADD COLUMN data_scope VARCHAR(20);
UPDATE rbac_roles SET data_scope = 'ADMIN' WHERE type = 'POSITION' AND name = 'ADMIN';
UPDATE rbac_roles SET data_scope = 'TEAM_LEAD' WHERE type = 'POSITION' AND name = 'TEAM_LEAD';
UPDATE rbac_roles SET data_scope = 'MEMBER' WHERE type = 'POSITION' AND name IN ('AGENT', 'MEMBER');
```

---

**ìµœì¢… ?…ë°?´íŠ¸**: 2026-02-05  
**ë²„ì „**: v3.0.0  
**?‘ì„±??*: Identity Modulith Development Team
- PHONE_AGENT: 3ê°?
- CHAT_AGENT: 2ê°?
- EMAIL_AGENT: 1ê°?
- SUPERVISOR: 15ê°?

---

## ?‘¥ 6. agent_roles (?¬ìš©????•  ë§¤í•‘)

**ëª©ì **: ?¬ìš©?ì? ??• ???¤ë???ê´€ê³?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹/?ˆì‹œ |
|--------|------|------|------|----------------|
| **id** | BIGSERIAL | ??| ë§¤í•‘ ID (PK) | 1, 2, 3... |
| agent_id | VARCHAR(36) | ??| ?¬ìš©??ID (FK) | UUID |
| role_id | VARCHAR(36) | ??| ??•  ID (FK) | UUID |
| assigned_at | TIMESTAMP | ??| ? ë‹¹ ?¼ì‹œ | `2026-01-21 10:00:00` |

**?¸ë±??*: `(agent_id, role_id)` UK, `agent_id`, `role_id`  
**FK**: `agent_id` ??`agents(agent_id)` ON DELETE CASCADE  
**FK**: `role_id` ??`roles(role_id)` ON DELETE CASCADE

**?’¡ ?¬ìš©?ëŠ” ?¬ëŸ¬ ??• ???™ì‹œ??ê°€ì§????ˆìŠµ?ˆë‹¤**:
- ?? `TEAM_LEAD` + `INBOUND_AGENT` = ?€?¥ì´ë©´ì„œ ?¸ë°”?´ë“œ ?„í™” ?ë‹´??ê°€??
- ?? `AGENT` + `INBOUND_AGENT` + `CHAT_AGENT` = ë©€??ì±„ë„ ?ë‹´??

---

## ?”„ ?Œì´ë¸?ê°?ê´€ê³„ë„

```
org_departments (ë¶€??
    ??1:N (parent_dept_id)
org_departments (?˜ìœ„ ë¶€??
    ??1:N (dept_id)
user_agents (?¬ìš©??
    ??M:N (user_agent_roles)
rbac_roles (??• )
    ??M:N (rbac_role_permissions)
rbac_permissions (ê¶Œí•œ)
```

---

## ?? ì´ˆê¸°??ë°©ë²•

### 1. ? í”Œë¦¬ì??´ì…˜ ?¤í–‰ (ê¶Œì¥)
```bash
./gradlew bootRun
```

Flywayê°€ ?ë™?¼ë¡œ `V1_0_0__Complete_Init.sql` ?¤í–‰ ??6ê°??Œì´ë¸?+ ?œì? ?°ì´???ì„±

### 2. ?˜ë™ ì´ˆê¸°??(?„ìš” ??
```bash
# PostgreSQL ?´ë¼?´ì–¸?¸ì—??
psql -U your_user -d your_database -f src/main/resources/db/migration/V1_0_0__Complete_Init.sql
```

### 3. ?•ì¸
```sql
SELECT 'org_departments' as table_name, COUNT(*) FROM org_departments
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'rbac_roles', COUNT(*) FROM rbac_roles
UNION ALL SELECT 'rbac_permissions', COUNT(*) FROM rbac_permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'agent_roles', COUNT(*) FROM agent_roles;
```

**?ˆìƒ ê²°ê³¼**: 16ë¶€?? 16?¬ìš©?? 8??• , 35ê¶Œí•œ, 77ë§¤í•‘, 22? ë‹¹

---

**ë¬¸ì„œ ë²„ì „**: 2.0.0 CLEAN  
**ìµœì¢… ?…ë°?´íŠ¸**: 2026-01-21


### ?¯ ?µì‹¬ ?¤ê³„ ?ì¹™

1. **UUID ê¸°ë°˜ ?ë³„??*: ëª¨ë“  ?”í‹°?°ëŠ” UUID ë¬¸ì??(VARCHAR(36)) ?¬ìš©
2. **ë©€?°í…Œ?Œì‹œ**: ëª¨ë“  ?Œì´ë¸”ì— `tenant_id` ì»¬ëŸ¼ ?¬í•¨
3. **Soft Delete**: ??• (`roles`)?€ `is_active` ?Œë˜ê·¸ë¡œ ?¼ë¦¬???? œ
4. **ê°ì‚¬ ì¶”ì **: ëª¨ë“  ê¶Œí•œ ë³€ê²½ì‚¬??? `audit_logs`??ê¸°ë¡
5. **ê³„ì¸µ êµ¬ì¡°**: ë¶€?œëŠ” ?ê¸°ì°¸ì¡° + org_pathë¡??¸ë¦¬ êµ¬í˜„

---

## 2. ?Œì´ë¸??ì„¸ ëª…ì„¸

### ?¢ 2.1 departmentEntities (ì¡°ì§/ë¶€??

**ëª©ì **: ì¡°ì§ ê³„ì¸µ êµ¬ì¡° ê´€ë¦?(?¸ë¦¬ êµ¬ì¡°)

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **dept_id** | VARCHAR(36) | NOT NULL | ë¶€??ID (PK) | UUID ?•ì‹ (`550e8400-...`) |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID | `tenant-001` ~ `tenant-999` |
| parent_id | VARCHAR(36) | NULL | ?ìœ„ ë¶€??ID (FK) | NULL = ìµœìƒ?? UUID = ?˜ìœ„ ë¶€??|
| name | VARCHAR(100) | NOT NULL | ë¶€?œëª… | ?œê?/?ë¬¸, 2-100??|
| org_path | VARCHAR(500) | NOT NULL | ì¡°ì§ ê²½ë¡œ | `/ë£¨íŠ¸ID/ë¶€?œID` ?•ì‹ |
| depth | INTEGER | NOT NULL | ?¸ë¦¬ ê¹Šì´ | 0(ìµœìƒ?? ~ 10(ìµœë?) |
| type | VARCHAR(50) | NULL | ë¶€???€??| `ë³¸ë?`, `?€`, `?ŒíŠ¸`, `?? ??|
| created_at | TIMESTAMP | NOT NULL | ?ì„± ?¼ì‹œ | `2026-01-20 10:30:00` |

**?¸ë±??*:
- UK: `(tenant_id, org_path)` - ê²½ë¡œ ì¤‘ë³µ ë°©ì?
- IDX: `tenant_id`, `parent_id`, `org_path`

**FK**:
- `parent_id` ??`departmentEntities(dept_id)` ON DELETE RESTRICT

**?°ì´???ˆì‹œ**:
```sql
-- ë³¸ë? (ìµœìƒ??
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL, 
 'ê²½ì˜ì§€?ë³¸ë¶€', '/d0000000-0000-0000-0000-000000000001', 0, 'ë³¸ë?', NOW())

-- ?€ (?˜ìœ„)
('d0000000-0000-0000-0000-000000000011', 'tenant-001', 
 'd0000000-0000-0000-0000-000000000001', '?¸ì‚¬?€', 
 '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011', 
 1, '?€', NOW())
```

---

### ?‘¤ 2.2 user_agents (?¬ìš©???ë‹´??

**ëª©ì **: ?œìŠ¤???¬ìš©???•ë³´ ê´€ë¦?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **agent_id** | VARCHAR(36) | NOT NULL | ?¬ìš©??ID (PK) | UUID ?•ì‹ |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID | `tenant-001` |
| login_id | VARCHAR(100) | NOT NULL | ë¡œê·¸??ID (UK) | ?ë¬¸+?«ì, 4-20??|
| password | VARCHAR(255) | NOT NULL | ë¹„ë?ë²ˆí˜¸ | BCrypt ?´ì‹œ (`$2a$10$...`) |
| name | VARCHAR(100) | NOT NULL | ?¬ìš©?ëª… | ?œê?/?ë¬¸, 2-50??|
| dept_id | VARCHAR(36) | NULL | ?Œì† ë¶€??ID (FK) | UUID ?ëŠ” NULL |
| status | VARCHAR(20) | NOT NULL | ?íƒœ | `ACTIVE`, `RETIRED` |
| password_must_change | BOOLEAN | NULL | ë¹„ë?ë²ˆí˜¸ ë³€ê²??„ìš” | `true`, `false` |
| created_at | TIMESTAMP | NOT NULL | ?ì„± ?¼ì‹œ | `2026-01-20 10:30:00` |
| updated_at | TIMESTAMP | NULL | ?˜ì • ?¼ì‹œ | `2026-01-20 15:00:00` |
| retired_at | TIMESTAMP | NULL | ?´ì§ ?¼ì‹œ | `2025-12-31 23:59:59` |
| job_title | VARCHAR(100) | NULL | ì§ì±… | `?€ë¦?, `ê³¼ì¥`, `?€?? ??|
| sync_status | VARCHAR(20) | NULL | ?™ê¸°???íƒœ | `SYNCED`, `PENDING` (Keycloak ?°ë™?? |
| role_id | VARCHAR(50) | NULL | ??•  ID (?ˆê±°?? | ?¬ìš© ì¤‘ë‹¨ ?ˆì • |

**?¸ë±??*:
- UK: `login_id`
- IDX: `tenant_id`, `dept_id`, `status`, `login_id`

**FK**:
- `dept_id` ??`departmentEntities(dept_id)` ON DELETE SET NULL

**?°ì´???œì?**:
- **login_id**: ?Œë¬¸??+ ?«ì ì¡°í•© (`admin`, `hong123`, `kim_gd`)
- **password**: BCrypt ?´ì‹œë§??€??(?‰ë¬¸ ?€??ê¸ˆì?)
- **status**: `ACTIVE`(?œì„±), `RETIRED`(?´ì§) ë§??¬ìš©
- **name**: ?¤ëª… ?¬ìš© ê¶Œì¥

---

### ?­ 2.3 roles (??• )

**ëª©ì **: RBAC ??•  ?•ì˜

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **role_id** | VARCHAR(36) | NOT NULL | ??•  ID (PK) | UUID ?•ì‹ |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID | `tenant-001` |
| name | VARCHAR(64) | NOT NULL | ??• ëª?(UK) | ?€ë¬¸ì+?¸ë”?¤ì½”?? 2-64??|
| type | VARCHAR(32) | NOT NULL | ??•  ?€??| `POSITION`, `CHANNEL`, `SKILL` |
| description | VARCHAR(255) | NULL | ??•  ?¤ëª… | ëª©ì  ë°?ê¶Œí•œ ë²”ìœ„ ?¤ëª… |
| is_active | BOOLEAN | NOT NULL | ?œì„±???íƒœ | `true`(?œì„±), `false`(ë¹„í™œ?? |
| version | BIGINT | NOT NULL | ?™ê???? ê¸ˆ ë²„ì „ | 0ë¶€???œì‘, ?˜ì • ??+1 |
| created_at | TIMESTAMP | NOT NULL | ?ì„± ?¼ì‹œ | `2026-01-20 10:30:00` |
| updated_at | TIMESTAMP | NOT NULL | ?˜ì • ?¼ì‹œ | `2026-01-20 15:00:00` |

**?¸ë±??*:
- UK: `(tenant_id, name)`
- IDX: `tenant_id`, `is_active`

**??•  ?€??(type)**:
- **POSITION**: ì§ê¸‰ ê¸°ë°˜ (?? `ADMIN`, `TEAM_LEADER`, `MEMBER`)
- **CHANNEL**: ì±„ë„ ê¸°ë°˜ (?? `INBOUND`, `OUTBOUND`, `CHAT`)
- **SKILL**: ?¤í‚¬ ê¸°ë°˜ (?? `VIP_SUPPORT`, `TECHNICAL_SUPPORT`)

**??• ëª?(name) ?œì?**:
```
- ?„ì²´ ê´€ë¦¬ì: ADMIN
- ?€?? TEAM_LEADER
- ?¼ë°˜ ?ë‹´?? AGENT
- ?¸ë°”?´ë“œ ?ë‹´: INBOUND_AGENT
- ?„ì›ƒë°”ìš´???ë‹´: OUTBOUND_AGENT
- ì±„íŒ… ?ë‹´: CHAT_AGENT
- VIP ?„ë‹´: VIP_AGENT
- ê¸°ìˆ  ì§€?? TECH_SUPPORT
```

---

### ?”‘ 2.4 permissions (ê¶Œí•œ)

**ëª©ì **: RBAC ê¶Œí•œ ?•ì˜

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **permission_id** | VARCHAR(36) | NOT NULL | ê¶Œí•œ ID (PK) | UUID ?•ì‹ |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID | `tenant-001` |
| code | VARCHAR(128) | NOT NULL | ê¶Œí•œ ì½”ë“œ (UK) | `domain:action` ?•ì‹ |
| created_at | TIMESTAMP | NOT NULL | ?ì„± ?¼ì‹œ | `2026-01-20 10:30:00` |

**?¸ë±??*:
- UK: `(tenant_id, code)`
- IDX: `tenant_id`

**ê¶Œí•œ ì½”ë“œ (code) ?œì?**:

?•ì‹: `{domain}:{action}`

**?„ë©”??(domain)**:
- `user`: ?¬ìš©??ê´€ë¦?
- `org`: ì¡°ì§ ê´€ë¦?
- `role`: ??•  ê´€ë¦?
- `permission`: ê¶Œí•œ ê´€ë¦?
- `agent_role`: ?¬ìš©????•  ? ë‹¹ ê´€ë¦?
- `audit`: ê°ì‚¬ ë¡œê·¸ ì¡°íšŒ

**?¡ì…˜ (action)**:
- `create`: ?ì„±
- `read`: ì¡°íšŒ
- `read:self`: ë³¸ì¸ë§?ì¡°íšŒ
- `update`: ?˜ì •
- `update:self`: ë³¸ì¸ë§??˜ì •
- `delete`: ?? œ
- `manage`: ?„ì²´ ê´€ë¦?
- `assign`: ? ë‹¹
- `view`: ë³´ê¸°

**?œì? ê¶Œí•œ ì½”ë“œ ?ˆì‹œ**:
```
user:create          - ?¬ìš©???ì„±
user:read            - ëª¨ë“  ?¬ìš©??ì¡°íšŒ
user:read:self       - ë³¸ì¸ ?•ë³´ë§?ì¡°íšŒ
user:update          - ?¬ìš©???•ë³´ ?˜ì •
user:delete          - ?¬ìš©???? œ
user:manage          - ?¬ìš©???„ì²´ ê´€ë¦?
user:assign:role     - ?¬ìš©?ì—ê²???•  ? ë‹¹
org:view             - ì¡°ì§??ë³´ê¸°
org:create           - ë¶€???ì„±
org:update           - ë¶€???•ë³´ ?˜ì •
org:move             - ë¶€???´ë™
org:delete           - ë¶€???? œ
role:create          - ??•  ?ì„±
role:read            - ??•  ì¡°íšŒ
role:update          - ??•  ?˜ì •
role:delete          - ??•  ?? œ
role:assign          - ??• ??ê¶Œí•œ ? ë‹¹
permission:create    - ê¶Œí•œ ?ì„±
permission:read      - ê¶Œí•œ ì¡°íšŒ
audit:view           - ê°ì‚¬ ë¡œê·¸ ì¡°íšŒ
```

---

### ?”— 2.5 role_permissions (??• -ê¶Œí•œ ë§¤í•‘)

**ëª©ì **: ??• ê³?ê¶Œí•œ???¤ë???ê´€ê³?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **id** | BIGSERIAL | NOT NULL | ë§¤í•‘ ID (PK) | ?ë™ ì¦ê? |
| role_id | VARCHAR(36) | NOT NULL | ??•  ID (FK) | UUID ?•ì‹ |
| permission_id | VARCHAR(36) | NOT NULL | ê¶Œí•œ ID (FK) | UUID ?•ì‹ |
| assigned_at | TIMESTAMP | NOT NULL | ? ë‹¹ ?¼ì‹œ | `2026-01-20 10:30:00` |

**?¸ë±??*:
- UK: `(role_id, permission_id)` - ì¤‘ë³µ ? ë‹¹ ë°©ì?

**FK**:
- `role_id` ??`roles(role_id)` ON DELETE CASCADE
- `permission_id` ??`permissions(permission_id)` ON DELETE CASCADE

---

### ?‘¥ 2.6 agent_roles (?¬ìš©????•  ë§¤í•‘)

**ëª©ì **: ?¬ìš©?ì? ??• ???¤ë???ê´€ê³?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **id** | BIGSERIAL | NOT NULL | ë§¤í•‘ ID (PK) | ?ë™ ì¦ê? |
| agent_id | VARCHAR(36) | NOT NULL | ?¬ìš©??ID (FK) | UUID ?•ì‹ |
| role_id | VARCHAR(36) | NOT NULL | ??•  ID (FK) | UUID ?•ì‹ |
| assigned_at | TIMESTAMP | NOT NULL | ? ë‹¹ ?¼ì‹œ | `2026-01-20 10:30:00` |

**?¸ë±??*:
- UK: `(agent_id, role_id)` - ì¤‘ë³µ ? ë‹¹ ë°©ì?
- IDX: `agent_id`, `role_id`

**FK**:
- `agent_id` ??`agents(agent_id)` ON DELETE CASCADE
- `role_id` ??`roles(role_id)` ON DELETE CASCADE

---

### ?“ 2.7 audit_logs (ê°ì‚¬ ë¡œê·¸)

**ëª©ì **: ê¶Œí•œ ê´€??ëª¨ë“  ë³€ê²½ì‚¬??ì¶”ì 

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… | ?œì? ?•ì‹ |
|--------|------|------|------|-----------|
| **audit_id** | VARCHAR(36) | NOT NULL | ê°ì‚¬ ë¡œê·¸ ID (PK) | UUID ?•ì‹ |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID | `tenant-001` |
| action | VARCHAR(32) | NOT NULL | ?‘ì—… ? í˜• | `CREATE`, `UPDATE`, `DELETE`, `ASSIGN`, `REVOKE` |
| resource_type | VARCHAR(64) | NOT NULL | ?€??ë¦¬ì†Œ???€??| `ROLE`, `PERMISSION`, `AGENT_ROLE` |
| resource_id | VARCHAR(255) | NOT NULL | ?€??ë¦¬ì†Œ??ID | UUID ?ëŠ” ë³µí•© ID |
| operator_id | VARCHAR(255) | NOT NULL | ?‘ì—… ?˜í–‰??ID | ?¬ìš©??UUID |
| changes | TEXT | NULL | ë³€ê²??´ìš© | JSON ?•ì‹ |
| timestamp | TIMESTAMP | NOT NULL | ?‘ì—… ?¼ì‹œ | `2026-01-20 10:30:00.123` |
| remarks | TEXT | NULL | ì¶”ê? ?•ë³´ | ë©”ëª¨, ?¤íŒ¨ ?ì¸ ??|
| ip_address | VARCHAR(45) | NULL | ?´ë¼?´ì–¸??IP | `192.168.1.100`, IPv6 ?¬í•¨ |

**?¸ë±??*:
- IDX: `tenant_id`, `resource_type`, `operator_id`, `timestamp DESC`

**?‘ì—… ? í˜• (action) ?œì?**:
- `CREATE`: ?ì„± (??• , ê¶Œí•œ)
- `UPDATE`: ?˜ì •
- `DELETE`: ?? œ
- `ASSIGN`: ? ë‹¹ (??• -ê¶Œí•œ, ?¬ìš©????• )
- `REVOKE`: ?Œìˆ˜

**ë¦¬ì†Œ???€??(resource_type) ?œì?**:
- `ROLE`: ??• 
- `PERMISSION`: ê¶Œí•œ
- `ROLE_PERMISSION`: ??• -ê¶Œí•œ ë§¤í•‘
- `AGENT_ROLE`: ?¬ìš©????•  ë§¤í•‘

**ë³€ê²??´ìš© (changes) JSON ?•ì‹**:
```json
// ??•  ?ì„±
{"roleName": "TEAM_LEADER", "roleType": "POSITION"}

// ??•  ?˜ì •
{"old": {"isActive": true}, "new": {"isActive": false}}

// ??• -ê¶Œí•œ ? ë‹¹
{"roleId": "uuid-role", "permissionId": "uuid-perm", "permissionCode": "user:create"}

// ?¬ìš©????•  ? ë‹¹
{"agentId": "uuid-agent", "roleId": "uuid-role", "roleName": "ADMIN"}
```

---

### ?—„ï¸?2.8 audit_logs_archive (ê°ì‚¬ ë¡œê·¸ ?„ì¹´?´ë¸Œ)

**ëª©ì **: 6ê°œì›” ?´ìƒ ?¤ë˜??ê°ì‚¬ ë¡œê·¸ ë³´ê?

| ì»¬ëŸ¼ëª?| ?€??| NULL | ?¤ëª… |
|--------|------|------|------|
| audit_id ~ ip_address | (audit_logs?€ ?™ì¼) | | |
| archived_at | TIMESTAMP | NOT NULL | ?„ì¹´?´ë¸Œ ?¼ì‹œ |

**?°ì´???´ë™**:
- ë§¤ì›” 1???ì • ?ë™ ?´ë™ (AuditLogArchivingBatchService)
- 6ê°œì›” ?´ì „ ?°ì´???€??

---

## 3. ?°ì´???œì???ê·œì¹™

### ?¯ 3.1 UUID ?ì„± ê·œì¹™

**?•ì‹**: `8-4-4-4-12` (ì´?36?? ?˜ì´???¬í•¨)
**?ˆì‹œ**: `550e8400-e29b-41d4-a716-446655440001`

**?ì„± ë°©ë²•**:
```java
// Java
UUID.randomUUID().toString()

// PostgreSQL
gen_random_uuid()::text
```

### ?·ï¸?3.2 ?Œë„Œ??ID ê·œì¹™

**?•ì‹**: `tenant-{?«ì 3?ë¦¬}`
**?ˆì‹œ**: `tenant-001`, `tenant-002`
**ë²”ìœ„**: `tenant-001` ~ `tenant-999`

### ?‘¤ 3.3 ?¬ìš©??ë¡œê·¸??ID ê·œì¹™

**?•ì‹**: ?ë¬¸ ?Œë¬¸??+ ?«ì + ?¸ë”?¤ì½”??
**ê¸¸ì´**: 4-20??
**?ˆì‹œ**: `admin`, `hong123`, `kim_gd`, `team_leader`
**ê¸ˆì?**: ?¹ìˆ˜ë¬¸ì (@, #, $ ??, ê³µë°±, ?œê?

### ?” 3.4 ë¹„ë?ë²ˆí˜¸ ê·œì¹™

**?€??*: BCrypt ?´ì‹œë§??€??
**?•ì‹**: `$2a$10$...` (60??
**Java ?ì„±**:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode("?ë³¸ë¹„ë?ë²ˆí˜¸");
```

### ?­ 3.5 ??• ëª?ê·œì¹™

**?•ì‹**: ?€ë¬¸ì + ?¸ë”?¤ì½”??
**ê¸¸ì´**: 2-64??
**?ˆì‹œ**: `ADMIN`, `TEAM_LEADER`, `INBOUND_AGENT`
**ê¸ˆì?**: ?Œë¬¸?? ê³µë°±, ?¹ìˆ˜ë¬¸ì

### ?”‘ 3.6 ê¶Œí•œ ì½”ë“œ ê·œì¹™

**?•ì‹**: `{domain}:{action}`
**domain**: ?Œë¬¸?? ?¸ë”?¤ì½”???ˆìš©
**action**: ?Œë¬¸?? ?¸ë”?¤ì½”???ˆìš©, ì½œë¡ (`:`) ?¤ì¤‘ ?ˆìš©
**?ˆì‹œ**: `user:create`, `org:read:team`, `role:assign`

### ?“… 3.7 ? ì§œ/?œê°„ ê·œì¹™

**?€??*: `TIMESTAMP WITHOUT TIME ZONE`
**?•ì‹**: `YYYY-MM-DD HH:MI:SS`
**?ˆì‹œ**: `2026-01-20 10:30:00`
**ê¸°ë³¸ê°?*: `NOW()` ?ëŠ” `CURRENT_TIMESTAMP`

---

## 4. ?Œì´ë¸?ê°?ê´€ê³„ë„

```
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
?? departmentEntities    ???„â??€?€?€?€??
?? (ì¡°ì§ ê³„ì¸µ)     ??      ???ê¸°ì°¸ì¡° (parent_id)
?”â??€?€?€?€?€?€?€?¬â??€?€?€?€?€?€?€??      ??
         ??               ??
         ??FK: dept_id    ??
         ??               ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??      ??
??    agents      ??      ??
??  (?¬ìš©??       ??      ??
?”â??€?€?€?€?€?€?€?¬â??€?€?€?€?€?€?€??      ??
         ??               ??
         ??FK: agent_id   ??
         ??               ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??      ??
?? agent_roles    ?‚â—„?€?€?€?€?€?€??
?? (?¤ë???ë§¤í•‘)   ??
?”â??€?€?€?€?€?€?€?¬â??€?€?€?€?€?€?€??
         ??FK: role_id
         ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??    roles       ??
??  (??• )        ??
?”â??€?€?€?€?€?€?€?¬â??€?€?€?€?€?€?€??
         ??FK: role_id
         ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
?‚role_permissions ??
?? (?¤ë???ë§¤í•‘)   ??
?”â??€?€?€?€?€?€?€?¬â??€?€?€?€?€?€?€??
         ??FK: permission_id
         ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
?? permissions    ??
??  (ê¶Œí•œ)        ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??

?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
?? audit_logs     ???€?€6ê°œì›” ?„â??€??audit_logs_archive
?? (ê°ì‚¬ ë¡œê·¸)     ??              (?„ì¹´?´ë¸Œ)
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
```

**CASCADE ê·œì¹™**:
- `role_permissions`: role ?? œ ??ë§¤í•‘???? œ
- `agent_roles`: agent ?ëŠ” role ?? œ ??ë§¤í•‘???? œ

**SET NULL ê·œì¹™**:
- `agents.dept_id`: departmentEntity ?? œ ??NULLë¡?ë³€ê²?

**RESTRICT ê·œì¹™**:
- `departmentEntities.parent_id`: ?˜ìœ„ ë¶€??ì¡´ì¬ ???? œ ë¶ˆê?

---

## 5. ?œì? ?°ì´???ˆì‹œ

### ?“¦ 5.1 ì´ˆê¸° ?°ì´?°ì…‹ êµ¬ì„±

**ë§ˆì´ê·¸ë ˆ?´ì…˜ ?¤í¬ë¦½íŠ¸**: `V1_0_9__Insert_Standard_Data.sql`

```
??ì¡°ì§ êµ¬ì¡° (3?¨ê³„ ê³„ì¸µ):
   - ë³¸ë? 3ê°?
   - ?€ 9ê°? 
   - ?ŒíŠ¸ 6ê°?
   - ì´?18ê°?ë¶€??

???¬ìš©??(16ëª?:
   - ?œì„± ?¬ìš©??15ëª?
   - ?´ì§ ?¬ìš©??1ëª?

??ê¶Œí•œ (35ê°?:
   - user: 9ê°?
   - org: 5ê°?
   - role: 7ê°?
   - permission: 4ê°?
   - agent_role: 4ê°?
   - audit: 6ê°?

????•  (8ê°?:
   - ADMIN (ìµœê³  ê´€ë¦¬ì)
   - TEAM_LEADER (?€??
   - AGENT (?¼ë°˜ ?ë‹´??
   - INBOUND_AGENT (?¸ë°”?´ë“œ)
   - OUTBOUND_AGENT (?„ì›ƒë°”ìš´??
   - CHAT_AGENT (ì±„íŒ… ?ë‹´)
   - VIP_AGENT (VIP ?„ë‹´)
   - TECH_SUPPORT (ê¸°ìˆ  ì§€??

????• -ê¶Œí•œ ë§¤í•‘ (77ê°?
???¬ìš©????•  ë§¤í•‘ (18ê°?
```

### ?¢ 5.2 ì¡°ì§ êµ¬ì¡° ?ˆì‹œ

```sql
-- ìµœìƒ??(ë³¸ë?)
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL,
 'ê²½ì˜ì§€?ë³¸ë¶€', '/d0000000-0000-0000-0000-000000000001', 0, 'ë³¸ë?', NOW())

-- 2?¨ê³„ (?€)
('d0000000-0000-0000-0000-000000000011', 'tenant-001',
 'd0000000-0000-0000-0000-000000000001',
 '?¸ì‚¬?€', '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011',
 1, '?€', NOW())

-- 3?¨ê³„ (?ŒíŠ¸)
('d0000000-0000-0000-0000-000000000111', 'tenant-001',
 'd0000000-0000-0000-0000-000000000011',
 'ì±„ìš©?ŒíŠ¸', '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011/d0000000-0000-0000-0000-000000000111',
 2, '?ŒíŠ¸', NOW())
```

### ?‘¤ 5.3 ?¬ìš©???°ì´???ˆì‹œ

```sql
INSERT INTO agents VALUES
-- ìµœê³  ê´€ë¦¬ì
('a0000000-0000-0000-0000-000000000001', 'tenant-001', 'admin',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
 '?œìŠ¤??ê´€ë¦¬ì', NULL, 'ACTIVE', false, NOW(), NULL, NULL, '?œìŠ¤??ê´€ë¦¬ì', NULL, NULL),

-- ?€??
('a0000000-0000-0000-0000-000000000002', 'tenant-001', 'teamlead01',
 '$2a$10$...', 'ê¹€?€??, 'd0000000-0000-0000-0000-000000000011',
 'ACTIVE', false, NOW(), NULL, NULL, '?€??, NULL, NULL),

-- ?¼ë°˜ ?ë‹´??
('a0000000-0000-0000-0000-000000000003', 'tenant-001', 'agent01',
 '$2a$10$...', '?´ìƒ??, 'd0000000-0000-0000-0000-000000000021',
 'ACTIVE', false, NOW(), NULL, NULL, '?€ë¦?, NULL, NULL);
```

### ?­ 5.4 ??• -ê¶Œí•œ ë§¤í•‘ ?ˆì‹œ

```sql
-- ADMIN ??• ??ëª¨ë“  ê¶Œí•œ ? ë‹¹
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'r0000000-0000-0000-0000-000000000001', permission_id, NOW()
FROM permissions WHERE tenant_id = 'tenant-001';

-- TEAM_LEADER ??• ???€ ê´€ë¦?ê¶Œí•œ ? ë‹¹
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'r0000000-0000-0000-0000-000000000002', permission_id, NOW()
FROM permissions 
WHERE tenant_id = 'tenant-001'
  AND code IN ('user:read', 'org:view', 'org:update');
```

### ?‘¥ 5.5 ?¬ìš©????•  ë§¤í•‘ ?ˆì‹œ

```sql
-- admin ?¬ìš©?ì—ê²?ADMIN ??•  ? ë‹¹
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'r0000000-0000-0000-0000-000000000001', NOW());

-- teamlead01 ?¬ìš©?ì—ê²?TEAM_LEADER ??•  ? ë‹¹
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000002', 'r0000000-0000-0000-0000-000000000002', NOW());

-- ?¤ì¤‘ ??•  ? ë‹¹ ?ˆì‹œ (?ë‹´??+ VIP ?„ë‹´)
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000003', 'r0000000-0000-0000-0000-000000000003', NOW()),
('a0000000-0000-0000-0000-000000000003', 'r0000000-0000-0000-0000-000000000007', NOW());
```

---

## ?” ë¶€ë¡? ? ìš©??SQL ì¿¼ë¦¬

### A. ì¡°ì§???„ì²´ ì¡°íšŒ (ê³„ì¸µ êµ¬ì¡°)
```sql
WITH RECURSIVE org_tree AS (
  SELECT dept_id, name, parent_id, 0 AS level, name AS path
  FROM departmentEntities
  WHERE tenant_id = 'tenant-001' AND parent_id IS NULL
  
  UNION ALL
  
  SELECT d.dept_id, d.name, d.parent_id, o.level + 1,
         o.path || ' > ' || d.name
  FROM departmentEntities d
  INNER JOIN org_tree o ON d.parent_id = o.dept_id
)
SELECT * FROM org_tree ORDER BY path;
```

### B. ?¬ìš©?ë³„ ê¶Œí•œ ì¡°íšŒ
```sql
SELECT a.login_id, a.name, r.name AS role_name, p.code AS permission_code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN roles r ON ar.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.tenant_id = 'tenant-001'
  AND a.status = 'ACTIVE'
ORDER BY a.login_id, r.name, p.code;
```

### C. ê°ì‚¬ ë¡œê·¸ ì¡°íšŒ (ìµœê·¼ 7??
```sql
SELECT audit_id, action, resource_type, operator_id, timestamp, changes
FROM audit_logs
WHERE tenant_id = 'tenant-001'
  AND timestamp >= NOW() - INTERVAL '7 days'
ORDER BY timestamp DESC
LIMIT 100;
```

### D. ë¶€?œë³„ ?¸ì› ì§‘ê³„
```sql
SELECT d.name AS dept_name, COUNT(a.agent_id) AS agent_count
FROM departmentEntities d
LEFT JOIN agents a ON d.dept_id = a.dept_id AND a.status = 'ACTIVE'
WHERE d.tenant_id = 'tenant-001'
GROUP BY d.dept_id, d.name
ORDER BY d.org_path;
```

---

## ?“Œ ì¤‘ìš” ì°¸ê³ ?¬í•­

### ? ï¸ ì£¼ì˜?¬í•­

1. **UUID ?¼ê???*: ëª¨ë“  ?”í‹°??ID??UUID (VARCHAR(36)) ?¬ìš©
2. **?Œë„Œ??ê²©ë¦¬**: ëª¨ë“  ì¿¼ë¦¬??`tenant_id` ì¡°ê±´ ?„ìˆ˜
3. **Soft Delete**: ??• ?€ `is_active = false`ë¡??¼ë¦¬???? œ
4. **CASCADE ì£¼ì˜**: ??• /ê¶Œí•œ ?? œ ??ë§¤í•‘ ?Œì´ë¸??ë™ ?? œ??
5. **ê°ì‚¬ ë¡œê·¸**: ëª¨ë“  ê¶Œí•œ ë³€ê²½ì? ?ë™?¼ë¡œ `audit_logs`??ê¸°ë¡

### ?“‹ ì²´í¬ë¦¬ìŠ¤??

?„ë¡œ?•ì…˜ ë°°í¬ ???•ì¸:
- [ ] ëª¨ë“  FK ?œì•½ì¡°ê±´ ?•ì¸
- [ ] ?¸ë±???±ëŠ¥ ?ŒìŠ¤??
- [ ] ?Œë„Œ??ê²©ë¦¬ ê²€ì¦?
- [ ] ê°ì‚¬ ë¡œê·¸ ?„ì¹´?´ë¹™ ?¤ì?ì¤??¤ì •
- [ ] ë°±ì—… ?•ì±… ?˜ë¦½

---

## ?“š ê´€??ë¬¸ì„œ

- [AUDIT_AND_CONSTANTS_ANALYSIS.md](./AUDIT_AND_CONSTANTS_ANALYSIS.md) - ê°ì‚¬ ë¡œê·¸ & ?ìˆ˜ ë¶„ì„
- [V1_0_0__Complete_Init.sql](./src/main/resources/db/migration/V1_0_0__Complete_Init.sql) - DB ì´ˆê¸°???¤í¬ë¦½íŠ¸
- [V1_0_9__Insert_Standard_Data.sql](./src/main/resources/db/migration/V1_0_9__Insert_Standard_Data.sql) - ?œì? ?°ì´???½ì…

---

**ë¬¸ì„œ ë²„ì „**: 2.0
**ìµœì¢… ê²€ì¦ì¼**: 2026-01-20
**?‘ì„±??*: Identity Modulith Team

> **ëª©ì **: ?°ì´?°ë² ?´ìŠ¤ ?¤ê³„, ?Œì´ë¸?êµ¬ì¡°, ?œì? ?°ì´?°ë? ??ê³³ì—???•ì¸  
> **?€??*: ê°œë°œ?€, ?´ì˜?€  
> **ë²„ì „**: 2.0  
> **ìµœì¢… ?˜ì •??*: 2026-01-16

---

## ?“‹ ëª©ì°¨
1. [?°ì´?°ë² ?´ìŠ¤ ê°œìš”](#?°ì´?°ë² ?´ìŠ¤-ê°œìš”)
2. [?Œì´ë¸?êµ¬ì¡°](#?Œì´ë¸?êµ¬ì¡°)
3. [?Œì´ë¸??ì„¸ ëª…ì„¸](#?Œì´ë¸??ì„¸-ëª…ì„¸)
4. [?Œì´ë¸?ê°??°ê?ê´€ê³?(#?Œì´ë¸?ê°??°ê?ê´€ê³?
5. [ì»¬ëŸ¼ ?°ì´???•ì‹ ?œì?](#ì»¬ëŸ¼-?°ì´???•ì‹-?œì?)
6. [?œì? ?°ì´??ê°€?´ë“œ](#?œì?-?°ì´??ê°€?´ë“œ)
7. [ê¶Œí•œ ë°???•  ?œì?](#ê¶Œí•œ-ë°???• -?œì?)

---

## ?°ì´?°ë² ?´ìŠ¤ ê°œìš”

### ?¤ê³„ ëª©í‘œ
- **ë©€?°í…Œ?Œì‹œ(Multi-Tenancy)**: ê°??Œì´ë¸”ì— tenant_idë¡??°ì´??ê²©ë¦¬
- **UUID ?µì¼**: ëª¨ë“  ?”í‹°??ID??UUID (VARCHAR(36))ë¡??µì¼
- **ì¡°ì§ ?¸ë¦¬**: ?ê¸°ì°¸ì¡°ë¥??´ìš©??ë¶€??ê³„ì¸µ êµ¬ì¡°
- **RBAC**: ??•  ê¸°ë°˜ ?‘ê·¼ ?œì–´ (Role-Based Access Control)
- **ê°ì‚¬ ì¶”ì **: ëª¨ë“  ?Œì´ë¸”ì— created_at, ì£¼ìš” ?‘ì—…?€ audit_logsë¡?ê¸°ë¡

### ?µì‹¬ ?ì¹™
```
??ID ?€?? UUID (VARCHAR(36)) ?µì¼
???¤ë???ê´€ê³? ì¤‘ê°„ ?Œì´ë¸”ë¡œ ëª…ì‹œ??ê´€ë¦?
???ê¸°ì°¸ì¡°: departments??parent_id
??Soft Delete: agents??status?€ retired_at
???°ì´??ê²©ë¦¬: ëª¨ë“  ?Œì´ë¸”ì— tenant_id (NOT NULL)
```

---

## ?Œì´ë¸?êµ¬ì¡°

### ?„ì²´ ?Œì´ë¸?ëª©ë¡ (6ê°?+ 2ê°?

| ?Œì´ë¸?| ëª¨ë“ˆ | ?©ë„ | PK ?€??| ì°¸ê³  |
|--------|------|------|---------|------|
| **departmentEntities** | Organization | ì¡°ì§/ë¶€??ê³„ì¸µ | VARCHAR(36) | ?ê¸°ì°¸ì¡° ?¸ë¦¬ |
| **agents** | User | ?¬ìš©??ì§ì› | VARCHAR(36) | Soft Delete |
| **roles** | RBAC | ??•  ?•ì˜ | VARCHAR(36) | ê¶Œí•œ ë¬¶ìŒ |
| **permissions** | RBAC | ê¶Œí•œ ?•ì˜ | VARCHAR(36) | ìµœì†Œ ?¨ìœ„ ê¶Œí•œ |
| **role_permissions** | RBAC | ??• -ê¶Œí•œ ë§¤í•‘ | BIGSERIAL | N:M ì¤‘ê°„ ?Œì´ë¸?|
| **agent_roles** | RBAC | ?¬ìš©????•  ë§¤í•‘ | BIGSERIAL | N:M ì¤‘ê°„ ?Œì´ë¸?|
| **audit_logs** | Audit | ê°ì‚¬ ë¡œê·¸ | BIGSERIAL | ë³€ê²??´ë ¥ ì¶”ì  |
| **audit_archive** | Audit | ê°ì‚¬ ë¡œê·¸ ?„ì¹´?´ë¸Œ | BIGSERIAL | 90???´ìƒ ë¡œê·¸ |

### ERD (Entity Relationship Diagram)

```
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??              Identity Modulith Database ERD                 ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??

?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??  departmentEntities    ??(?ê¸°ì°¸ì¡°)
?œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??PK: dept_id (U)  ??
??    tenant_id    ??
??FK: parent_id ?€?€?€?¼â??€?€?€?€??
??    name         ??    ??
??    org_path     ??    ??
??    depth        ??    ??
??    type         ??    ??
??    created_at   ??    ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??    ??
         ??              ??
         ??1:N (?ê¸°ì°¸ì¡°)|
         ?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€??
         ??
         ??1:N (?Œì†)
         ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??    agents       ??
?œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??PK: agent_id (U) ??
??    tenant_id    ??
??    login_id (U) ??
??    password     ??
??    name         ??
??FK: dept_id ?€?€?€?€?€??
??    status       ??
??    ...etc       ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
         ??
         ??N:M (??•  ? ë‹¹)
         ??
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??      ?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??  agent_roles    ??      ??     roles       ??
?œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??      ?œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
??PK: id           ??      ??PK: role_id (U)  ??
??FK: agent_id ?€?€?€?€?¼â??€?€?€?€?€?’â”‚     tenant_id    ??
??FK: role_id ?€?€?€?€?€?¼â??€?€?€?€?€?’â”‚     name (U)     ??
??    assigned_at  ??      ??    type         ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??      ??    created_at   ??
                           ?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
                                   ??
                                   ??N:M
                                   ??
                           ?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
                           ??role_permissions ??
                           ?œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
                           ??PK: id           ??
                           ??FK: role_id ?€?€?€?€?€??
                           ??FK: permission_id??
                           ??    assigned_at  ??
                           ?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
                                   ??
                                   ??
                           ?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
                           ??  permissions    ??
                           ?œâ??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??
                           ??PK: permission_id??
                           ??    tenant_id    ??
                           ??    code (U)     ??
                           ??    created_at   ??
                           ?”â??€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€?€??

ë²”ë?: PK=Primary Key, FK=Foreign Key, U=UUID, N:M=?¤ë???
```

---

## ?Œì´ë¸??ì„¸ ëª…ì„¸

### 1. departmentEntities (ì¡°ì§/ë¶€???Œì´ë¸?

**ëª©ì **: ?Œì‚¬ ì¡°ì§ ê³„ì¸µ êµ¬ì¡° ê´€ë¦?(?¸ë¦¬ êµ¬ì¡°)

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| dept_id | VARCHAR(36) | PK | ë¶€??ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID (ë©€?°í…Œ?Œì‹œ) |
| parent_id | VARCHAR(36) | FK (?ê¸°ì°¸ì¡°) | ?ìœ„ ë¶€??ID (NULL?´ë©´ ìµœìƒ?? |
| name | VARCHAR(100) | NOT NULL | ë¶€?œëª… |
| org_path | VARCHAR(500) | NOT NULL, UNIQUE | ì¡°ì§ ê²½ë¡œ (?? /dept1/dept2/dept3) |
| depth | INTEGER | NOT NULL | ?¸ë¦¬ ê¹Šì´ (0ë¶€???œì‘) |
| type | VARCHAR(50) | | ë¶€???€??(HEADQUARTERS, DIVISION, TEAM) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?ì„± ?¼ì‹œ |

**?¸ë±??*:
- PK: dept_id
- UK: (tenant_id, org_path)
- FK: parent_id ??dept_id (?ê¸°ì°¸ì¡°, ON DELETE RESTRICT)
- IDX: (tenant_id), (parent_id), (org_path)

**?¹ì§•**:
- **?ê¸°ì°¸ì¡° (Self-Join)**: parent_idë¡??í•˜ ê´€ê³??œí˜„
- **Closure Table ?€??*: org_pathë¡?ê³„ì¸µ ?ìƒ‰ ìµœì ??
- **?? œ ?œì•½**: RESTRICTë¡??˜ìœ„ ë¶€???ˆìœ¼ë©??? œ ë¶ˆê?

**?ˆì‹œ ?°ì´??*:
```sql
dept_id              | name        | parent_id | org_path          | depth | type
---------------------|-------------|-----------|-------------------|-------|-------------
d50e8400-e29b-...001 | ?¥ìŠ¤?„ë¡ ë³¸ë? | NULL      | /d50e...001       | 0     | HEADQUARTERS
d50e8400-e29b-...002 | ê³ ê°ì§€?ì‚¬ë¶€ | ...001    | /d50e...001/002   | 1     | DIVISION
d50e8400-e29b-...005 | ?„í™”?ë‹´?€  | ...002    | /d50e...001/002/005 | 2   | TEAM
```

---

### 2. user_agents (?¬ìš©??ì§ì› ?Œì´ë¸?

**ëª©ì **: ?œìŠ¤???¬ìš©???•ë³´ ê´€ë¦?

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| agent_id | VARCHAR(36) | PK | ?¬ìš©??ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID |
| login_id | VARCHAR(100) | NOT NULL, UNIQUE | ë¡œê·¸??ID |
| password | VARCHAR(255) | NOT NULL | ë¹„ë?ë²ˆí˜¸ (BCrypt ?´ì‹œ) |
| name | VARCHAR(100) | NOT NULL | ?¬ìš©?ëª… |
| dept_id | VARCHAR(36) | FK | ?Œì† ë¶€??ID (NULL ê°€?? |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ?íƒœ (ACTIVE, RETIRED) |
| password_must_change | BOOLEAN | DEFAULT false | ë¹„ë?ë²ˆí˜¸ ë³€ê²??„ìš” ?¬ë? |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?ì„± ?¼ì‹œ |
| updated_at | TIMESTAMP | | ?˜ì • ?¼ì‹œ |
| retired_at | TIMESTAMP | | ?´ì§ ?¼ì‹œ |
| job_title | VARCHAR(100) | | ì§ì±… |
| sync_status | VARCHAR(20) | | ?™ê¸° ?íƒœ |

**?¸ë±??*:
- PK: agent_id
- UK: login_id
- FK: dept_id ??departmentEntities.dept_id (ON DELETE SET NULL)
- IDX: (tenant_id), (dept_id), (status), (login_id)

**?¹ì§•**:
- **Soft Delete**: status='RETIRED'ë¡??¼ë¦¬???? œ (ë¬¼ë¦¬???? œ X)
- **?¤ì¤‘ ??• **: agent_roles ?Œì´ë¸”ë¡œ ?¬ëŸ¬ ??•  ? ë‹¹ ê°€??
- **ë¶€???°ê²°**: dept_idë¡?ì¡°ì§ êµ¬ì¡°?€ ?°ê²°

**?ˆì‹œ ?°ì´??*:
```sql
agent_id             | login_id    | name      | dept_id      | status
---------------------|-------------|-----------|--------------|--------
550e8400-e29b-...101 | admin       | ?œìŠ¤?œê?ë¦¬ì | d50e...001  | ACTIVE
550e8400-e29b-...104 | phone_ag01 | ë°•ìƒ??    | d50e...005  | ACTIVE
550e8400-e29b-...199 | retired_usr | ?´ì§??   | d50e...005  | RETIRED
```

---

### 3. roles (??•  ?Œì´ë¸?

**ëª©ì **: RBAC ??•  ?•ì˜ (ê¶Œí•œ ë¬¶ìŒ)

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| role_id | VARCHAR(36) | PK | ??•  ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | ??• ëª?(ADMIN, MANAGER ?? |
| type | VARCHAR(32) | NOT NULL | ??•  ?€??(POSITION, CHANNEL, SKILL) |
| description | VARCHAR(255) | | ??•  ?¤ëª… |
| is_active | BOOLEAN | DEFAULT true | ?œì„±???¬ë? |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?ì„± ?¼ì‹œ |

**?¸ë±??*:
- PK: role_id
- UK: (tenant_id, name)
- IDX: (tenant_id)

**??•  ë¶„ë¥˜**:

| ?€??| ?¤ëª… | ?ˆì‹œ |
|------|------|------|
| POSITION | ì§ê¸‰ ê¸°ë°˜ (ì§ì±…) | ADMIN, MANAGER, TEAM_LEAD, MEMBER |
| CHANNEL | ì±„ë„ ê¸°ë°˜ (?…ë¬´ ì±„ë„) | PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR |
| SKILL | ??Ÿ‰ ê¸°ë°˜ | (?•ì¥ ê°€?? |

**?¹ì§•**:
- **?¤ì¤‘ ??•  ì¡°í•©**: ?¬ìš©?ëŠ” POSITION + CHANNEL ì¡°í•© ê°€??
- ?? ë°•ìƒ??= MEMBER (ì§ê¸‰) + PHONE_AGENT (ì±„ë„)

**?ˆì‹œ ?°ì´??*:
```sql
role_id              | name         | type      | is_active
---------------------|--------------|-----------|----------
660e8400-e29b-...001 | ADMIN        | POSITION  | true
660e8400-e29b-...005 | PHONE_AGENT  | CHANNEL   | true
```

---

### 4. permissions (ê¶Œí•œ ?Œì´ë¸?

**ëª©ì **: ?œìŠ¤??ê¶Œí•œ ?•ì˜ (ìµœì†Œ ?¨ìœ„ ê¶Œí•œ)

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| permission_id | VARCHAR(36) | PK | ê¶Œí•œ ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID |
| code | VARCHAR(128) | NOT NULL, UNIQUE | ê¶Œí•œ ì½”ë“œ (domain:action ?•ì‹) |
| description | VARCHAR(255) | | ê¶Œí•œ ?¤ëª… |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?ì„± ?¼ì‹œ |

**?¸ë±??*:
- PK: permission_id
- UK: (tenant_id, code)
- IDX: (tenant_id)

**ê¶Œí•œ ì½”ë“œ ?•ì‹**:
```
{domain}:{action}[:{resource}]

?„ë©”??(8ê°?:
?œâ? user:      ?¬ìš©??ê´€ë¦?(9ê°?
?œâ? org:       ì¡°ì§ ê´€ë¦?(6ê°?
?œâ? rbac:      RBAC ê´€ë¦?(9ê°?
?œâ? report:    ë³´ê³ ??(4ê°?
?œâ? phone:     ?„í™” ì±„ë„ (3ê°?
?œâ? chat:      ì±„íŒ… ì±„ë„ (2ê°?
?œâ? email:     ?´ë©”??ì±„ë„ (1ê°?
?”â? queue:     ??ê´€ë¦?(1ê°?

ì´?35ê°?ê¶Œí•œ
```

**?ˆì‹œ ?°ì´??*:
```sql
permission_id        | code                | description
---------------------|---------------------|------------------
550e8400-e29b-...001 | user:create         | ?¬ìš©???ì„±
550e8400-e29b-...029 | phone:accept        | ?„í™” ?˜ë½
550e8400-e29b-...032 | chat:send           | ì±„íŒ… ?„ì†¡
```

---

### 5. role_permissions (??• -ê¶Œí•œ ë§¤í•‘ ?Œì´ë¸?

**ëª©ì **: ??• ??ê¶Œí•œ ? ë‹¹ (N:M ê´€ê³?

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| id | BIGSERIAL | PK | ë§¤í•‘ ID (?ë™ ì¦ê?) |
| role_id | VARCHAR(36) | FK, NOT NULL | ??•  ID |
| permission_id | VARCHAR(36) | FK, NOT NULL | ê¶Œí•œ ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ? ë‹¹ ?¼ì‹œ |

**?¸ë±??*:
- PK: id
- UK: (role_id, permission_id)
- FK: role_id ??roles.role_id (ON DELETE CASCADE)
- FK: permission_id ??permissions.permission_id (ON DELETE CASCADE)

**?¹ì§•**:
- **?¤ë???ê´€ê³?*: ????• ???¬ëŸ¬ ê¶Œí•œ ? ë‹¹ ê°€??
- **?™ì  ê¶Œí•œ ê´€ë¦?*: ??•  ë³€ê²????ë™ ë°˜ì˜
- **CASCADE ?? œ**: ??• /ê¶Œí•œ ?? œ ??ë§¤í•‘???ë™ ?? œ

**ê¶Œí•œ ë°°ë¶„ ?ˆì‹œ**:
```sql
ADMIN:     35ê°?(?„ì²´)
MANAGER:   12ê°?(?¬ìš©?? ì¡°ì§, ë³´ê³ ??
TEAM_LEAD:  5ê°?(?½ê¸°, ì¡°ì§ ë·? ë³´ê³ ??
MEMBER:     4ê°?(ë³¸ì¸ ?½ê¸°, ì¡°ì§ ë·? ë³´ê³ ??

PHONE_AGENT:  3ê°?(?„í™” ê´€??
CHAT_AGENT:   2ê°?(ì±„íŒ… ê´€??
EMAIL_AGENT:  1ê°?(?´ë©”??ê´€??
SUPERVISOR:   7ê°?(ëª¨ë“  ì±„ë„ + ??

ì´?77ê°?ë§¤í•‘
```

---

### 6. agent_roles (?¬ìš©????•  ë§¤í•‘ ?Œì´ë¸?

**ëª©ì **: ?¬ìš©?ì—ê²???•  ? ë‹¹ (N:M ê´€ê³?

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| id | BIGSERIAL | PK | ë§¤í•‘ ID (?ë™ ì¦ê?) |
| agent_id | VARCHAR(36) | FK, NOT NULL | ?¬ìš©??ID |
| role_id | VARCHAR(36) | FK, NOT NULL | ??•  ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ? ë‹¹ ?¼ì‹œ |

**?¸ë±??*:
- PK: id
- UK: (agent_id, role_id) - ì¤‘ë³µ ë°©ì?
- FK: agent_id ??agents.agent_id (ON DELETE CASCADE)
- FK: role_id ??roles.role_id (ON DELETE CASCADE)
- IDX: (agent_id), (role_id)

**?¹ì§•**:
- **?¤ì¤‘ ??• **: ?¬ìš©?ëŠ” ?¬ëŸ¬ ??•  ë³´ìœ  ê°€??(?? MEMBER + PHONE_AGENT + SUPERVISOR)
- **?™ì  ? ë‹¹**: ??•  ì¶”ê?/?œê±° ???ë™ ë°˜ì˜
- **ê¶Œí•œ ê³„ì‚°**: ëª¨ë“  ??• ??ê¶Œí•œ ?©ì§‘??= ?¬ìš©?ì˜ ìµœì¢… ê¶Œí•œ

**?ˆì‹œ ?°ì´??*:
```sql
agent_id (ë°•ìƒ??    | role_id (??• )
---------------------|----------------------
550e8400-e29b-...104 | 660e8400-e29b-...004 (MEMBER)
550e8400-e29b-...104 | 660e8400-e29b-...005 (PHONE_AGENT)
```

---

### 7. audit_logs (ê°ì‚¬ ë¡œê·¸ ?Œì´ë¸?

**ëª©ì **: ?œìŠ¤??ì£¼ìš” ?‘ì—… ?´ë ¥ ì¶”ì 

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| id | BIGSERIAL | PK | ë¡œê·¸ ID (?ë™ ì¦ê?) |
| tenant_id | VARCHAR(50) | NOT NULL | ?Œë„Œ??ID |
| action | VARCHAR(100) | NOT NULL | ?‘ì—… (ROLE_ASSIGNED, PERMISSION_CREATED ?? |
| target_type | VARCHAR(50) | NOT NULL | ?€???€??(ROLE, PERMISSION, USER ?? |
| target_id | VARCHAR(100) | NOT NULL | ?€??ID |
| actor_id | VARCHAR(36) | NOT NULL | ?‘ì—…??ID |
| details | TEXT | | ?ì„¸ ?•ë³´ (JSON ?•ì‹) |
| ip_address | VARCHAR(45) | | ?‘ì—…??IP |
| timestamp | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ë°œìƒ ?¼ì‹œ |

**?¸ë±??*:
- PK: id
- IDX: (tenant_id, timestamp), (actor_id), (target_type, target_id)

**?¹ì§•**:
- **ë¶ˆë? ë¡œê·¸**: ?ì„± ???˜ì •/?? œ ë¶ˆê?
- **90???ë™ ?„ì¹´?´ë¹™**: audit_archiveë¡??´ë™
- **JSON ?ì„¸ ?•ë³´**: ë³€ê²??„í›„ ê°??€??

---

### 8. audit_archive (ê°ì‚¬ ë¡œê·¸ ?„ì¹´?´ë¸Œ ?Œì´ë¸?

**ëª©ì **: 90???´ìƒ ?¤ë˜??ê°ì‚¬ ë¡œê·¸ ë³´ê?

| ì»¬ëŸ¼ëª?| ?€??| ?œì•½ | ?¤ëª… |
|--------|------|------|------|
| (audit_logs?€ ?™ì¼) | | | |
| archived_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?„ì¹´?´ë¹™ ?¼ì‹œ |

**?¹ì§•**:
- **?ë™ ?„ì¹´?´ë¹™**: ë°°ì¹˜ ?‘ì—…?¼ë¡œ 90??ì´ˆê³¼ ë¡œê·¸ ?´ë™
- **?¥ê¸° ë³´ê?**: ë²•ì  ?”êµ¬?¬í•­ ?€??
- **ê²€??ìµœì ??*: ìµœê·¼ ë¡œê·¸??audit_logs?ì„œë§?ê²€??

---

## ?Œì´ë¸?ê°??°ê?ê´€ê³?

### 1. ?¼ë???(One-to-Many) ê´€ê³?

#### departmentEntities (1) ??departmentEntities (N) - ?ê¸°ì°¸ì¡°
```
?ìœ„ ë¶€??(parent) ???˜ìœ„ ë¶€?œë“¤ (?ì‹)

ê´€ê³? ë¶€ëª??ì‹
FK: parent_id ??dept_id
?¹ì§•: ?ê¸°ì°¸ì¡°, ?¸ë¦¬ êµ¬ì¡°
?? œ ?•ì±…: ON DELETE RESTRICT (?˜ìœ„ ë¶€???ˆìœ¼ë©??? œ ë¶ˆê?)

?ˆì‹œ:
?¥ìŠ¤?„ë¡  ë³¸ë? (root)
?œâ? ê³ ê°ì§€?ì‚¬?…ë?
?? ?œâ? ?„í™”?ë‹´?€
?? ?”â? ì±„íŒ…?ë‹´?€
?”â? ê¸°ìˆ ê°œë°œë³¸ë?
   ?”â? Backendê°œë°œ?€
```

#### departmentEntities (1) ??user_agents (N)
```
ë¶€??(departmentEntity) ???Œì† ì§ì›??(employees)

ê´€ê³? ì¡°ì§ ?¬í•¨ ê´€ê³?
FK: agents.dept_id ??departmentEntities.dept_id
?¹ì§•: ?˜ë‚˜??ë¶€?œì— ?¬ëŸ¬ ì§ì›
?? œ ?•ì±…: ON DELETE SET NULL (ë¶€???? œ ??ì§ì›??dept_id = NULL)

?ˆì‹œ:
?„í™”?ë‹´?€ (1ê°?
?œâ? ?´í???(1ëª?
?œâ? ë°•ìƒ??(1ëª?
?”â? ìµœìƒ??(1ëª?
```

---

### 2. ?¤ë???(Many-to-Many) ê´€ê³?

#### user_agents (N) ??roles (M) via agent_roles
```
?¬ìš©???â†’ ??• 

êµ¬ì¡°:
agents ??agent_roles ??roles

?¹ì§•:
- ???¬ìš©?ê? ?¬ëŸ¬ ??•  ë³´ìœ 
- ????• ???¬ëŸ¬ ?¬ìš©?ì—ê²?? ë‹¹
- ì¤‘ê°„ ?Œì´ë¸? agent_roles

?ˆì‹œ:
ë°•ìƒ??(1ëª?
?œâ? MEMBER (ì§ê¸‰)
?”â? PHONE_AGENT (ì±„ë„)

MEMBER ??• 
?œâ? ë°•ìƒ??
?œâ? ?•ìƒ??
?œâ? ê°•ìƒ??
?”â? ... (7ëª?

?? œ ?•ì±…: ON DELETE CASCADE (?‘ìª½ ëª¨ë‘)
```

#### roles (N) ??permissions (M) via role_permissions
```
??•  ?â†’ ê¶Œí•œ

êµ¬ì¡°:
roles ??role_permissions ??permissions

?¹ì§•:
- ????• ???¬ëŸ¬ ê¶Œí•œ ?¬í•¨
- ??ê¶Œí•œ???¬ëŸ¬ ??• ??? ë‹¹ ê°€??
- ì¤‘ê°„ ?Œì´ë¸? role_permissions

?ˆì‹œ:
ADMIN ??•  (1ê°? ??35ê°?ê¶Œí•œ (ëª¨ë‘)
MEMBER ??•  (1ê°? ??4ê°?ê¶Œí•œ (ìµœì†Œ)

?? œ ?•ì±…: ON DELETE CASCADE (?‘ìª½ ëª¨ë‘)
```

---

### 3. ê¶Œí•œ ì²´í¬ ?ë¦„

**?¬ìš©?ì˜ ìµœì¢… ê¶Œí•œ ê³„ì‚°**:

```
1?¨ê³„: ?¬ìš©??ì¡°íšŒ
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€??
??agents       ??(agent_idë¡?ì¡°íšŒ)
??agent_id=... ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€??
        ??

2?¨ê³„: ?¬ìš©?ì˜ ëª¨ë“  ??•  ì¡°íšŒ
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€??
??agent_roles  ??(WHERE agent_id = ?)
??role_id=... ??
??role_id=... ??(?¤ì¤‘ ??• )
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€??
        ??

3?¨ê³„: ê°???• ??ëª¨ë“  ê¶Œí•œ ì¡°íšŒ
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€??
??role_permissions ??(WHERE role_id IN (...))
??permission_id=... ??
??permission_id=... ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€??
        ??

4?¨ê³„: ëª¨ë“  ê¶Œí•œ ì½”ë“œ ì¡°íšŒ
?Œâ??€?€?€?€?€?€?€?€?€?€?€?€?€??
??permissions  ??
??code='user:create' ??
??code='phone:accept' ??
?”â??€?€?€?€?€?€?€?€?€?€?€?€?€??
        ??

5?¨ê³„: ê¶Œí•œ ?•ì¸
ìµœì¢… ê¶Œí•œ = ëª¨ë“  ??• ??ê¶Œí•œ ?©ì§‘??(Union)
```

**SQL ?ˆì‹œ**:
```sql
-- ?¹ì • ?¬ìš©?ì˜ ëª¨ë“  ê¶Œí•œ ì¡°íšŒ
SELECT DISTINCT p.code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN role_permissions rp ON ar.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.agent_id = ? 
  AND a.status = 'ACTIVE'
  AND a.tenant_id = ?;
```

---

## ì»¬ëŸ¼ ?°ì´???•ì‹ ?œì?

### 1. ID ì»¬ëŸ¼ (ëª¨ë‘ UUIDë¡??µì¼)

| ì»¬ëŸ¼ëª?| ?€??| ?¬ê¸° | ?•ì‹ | ?ˆì‹œ |
|--------|------|------|------|------|
| dept_id | VARCHAR | 36 | UUID | d50e8400-e29b-41d4-a716-446655440001 |
| agent_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440101 |
| role_id | VARCHAR | 36 | UUID | 660e8400-e29b-41d4-a716-446655440001 |
| permission_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440001 |

**UUID ?•ì‹**:
```
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  8??  - 4??- 4??- 4??-    12??
  ì´?36??(?˜ì´???¬í•¨)
```

---

### 2. ë¬¸ì??ì»¬ëŸ¼ ?œì?

| ì»¬ëŸ¼ëª?| ìµœë?ê¸¸ì´ | ?¤ëª… | ?ˆì‹œ |
|--------|---------|------|------|
| tenant_id | 50 | ?Œë„Œ??ID (ê³ ì •) | tenant-001 |
| login_id | 100 | ë¡œê·¸??ID (?ìˆ«?? -, _) | phone_agent01 |
| password | 255 | BCrypt ?´ì‹œ | $2a$10$N9qo8... |
| name | 100 | ?¬ìš©??ë¶€?œëª… | ë°•ìƒ?? ?„í™”?ë‹´?€ |
| org_path | 500 | ì¡°ì§ ê²½ë¡œ (UUID ê¸°ë°˜) | /d50e8400.../d50e8400.../... |
| job_title | 100 | ì§ì±… | ?€?? ê³¼ì¥ |
| type (departmentEntities) | 50 | ë¶€???€??| HEADQUARTERS, DIVISION, TEAM |
| type (roles) | 32 | ??•  ?€??| POSITION, CHANNEL, SKILL |
| status (agents) | 20 | ?íƒœ | ACTIVE, RETIRED |
| name (roles) | 64 | ??• ëª?(?€ë¬¸ì, _) | ADMIN, TEAM_LEAD, PHONE_AGENT |
| code (permissions) | 128 | ê¶Œí•œ ì½”ë“œ (?„ë©”???¡ì…˜) | user:create, phone:accept |

---

### 3. ?œê°„ ì»¬ëŸ¼ ?œì?

| ì»¬ëŸ¼ëª?| ?€??| ?•ì‹ | ?¤ëª… | ?ˆì‹œ |
|--------|------|------|------|------|
| created_at | TIMESTAMP | ISO 8601 | ?ì„± ?¼ì‹œ (?ë™) | 2026-01-16 10:00:00 |
| updated_at | TIMESTAMP | ISO 8601 | ?˜ì • ?¼ì‹œ (?ë™) | 2026-01-16 10:05:00 |
| assigned_at | TIMESTAMP | ISO 8601 | ? ë‹¹ ?¼ì‹œ | 2026-01-16 10:00:00 |
| retired_at | TIMESTAMP | ISO 8601 | ?´ì§ ?¼ì‹œ (NULL ê°€?? | 2025-12-14 17:00:00 |
| timestamp | TIMESTAMP | ISO 8601 | ê°ì‚¬ ë¡œê·¸ ë°œìƒ ?¼ì‹œ | 2026-01-16 10:00:00 |

---

### 4. NULL ?•ì±…

| ì»¬ëŸ¼ëª?| ?Œì´ë¸?| NULL ?ˆìš© | ?´ìœ  | ë¹„ê³  |
|--------|--------|----------|------|------|
| parent_id | departmentEntities | YES | ìµœìƒ??ë¶€?œì¼ ???ˆìŒ | ë£¨íŠ¸??NULL |
| dept_id | agents | YES | ë¶€??ë¯¸ì • ì§ì› ê°€??| ON DELETE SET NULL |
| updated_at | agents | YES | ?ì„± ???˜ì • ?†ì„ ???ˆìŒ | ? íƒ?¬í•­ |
| retired_at | agents | YES | ?œì„± ì§ì›?€ NULL | Soft Delete |
| job_title | agents | YES | ì§ì±… ë¯¸ì • ê°€??| ? íƒ?¬í•­ |
| description | roles, permissions | YES | ?¤ëª… ? íƒ?¬í•­ | |
| ip_address | audit_logs | YES | IP ì¶”ì  ë¶ˆê??¥í•  ???ˆìŒ | |

---

## ?œì? ?°ì´??ê°€?´ë“œ

### ?œì? ?°ì´?°ì…‹

| ??ª© | ?˜ëŸ‰ | ?¤ëª… |
|------|------|------|
| **Departments** | 13ê°?| ë³¸ë?(1) + ?¬ì—…ë¶€(3) + ?€(9) |
| **Agents** | 16ëª?| ?œì„±(15) + ?´ì§(1) |
| **Roles** | 8ê°?| POSITION(4) + CHANNEL(4) |
| **Permissions** | 35ê°?| 8ê°??„ë©”??|
| **Role-Permissions** | 77ê°?| ??• ë³?ê¶Œí•œ ë§¤í•‘ |
| **Agent-Roles** | ~30ê°?| ?¬ìš©?ë³„ ?¤ì¤‘ ??•  |

### ì¡°ì§ êµ¬ì¡° ?ˆì‹œ

```
?¥ìŠ¤?„ë¡  ë³¸ë? (HEADQUARTERS)
?œâ? ê³ ê°ì§€?ì‚¬?…ë? (DIVISION)
?? ?œâ? ?„í™”?ë‹´?€ (TEAM)
?? ?? ?œâ? ?´í???(TEAM_LEAD + SUPERVISOR)
?? ?? ?œâ? ë°•ìƒ??(MEMBER + PHONE_AGENT)
?? ?? ?”â? ìµœìƒ??(MEMBER + PHONE_AGENT)
?? ?œâ? ì±„íŒ…?ë‹´?€ (TEAM)
?? ?”â? VIPê³ ê°ì§€?í? (TEAM)
?œâ? ?ì—…?¬ì—…ë¶€ (DIVISION)
?”â? ê¸°ìˆ ê°œë°œë³¸ë? (DIVISION)
   ?œâ? Backendê°œë°œ?€ (TEAM)
   ?œâ? Frontendê°œë°œ?€ (TEAM)
   ?”â? DevOps?€ (TEAM)
```

---

## ê¶Œí•œ ë°???•  ?œì?

### ê¶Œí•œ(Permission) ì½”ë“œ ê·œì¹™

**?•ì‹**: `{domain}:{action}[:{resource}]`

### ?„ë©”?¸ë³„ ê¶Œí•œ ëª©ë¡ (ì´?35ê°?

#### 1. ?¬ìš©??ê´€ë¦?(user, agent) - 9ê°?
```
- user:create          ?¬ìš©???ì„±
- user:read            ?¬ìš©??ì¡°íšŒ
- user:update          ?¬ìš©???˜ì •
- user:delete          ?¬ìš©???? œ
- user:read:self       ë³¸ì¸ ?•ë³´ ì¡°íšŒ
- user:update:self     ë³¸ì¸ ?•ë³´ ?˜ì •
- user:assign:role     ??•  ? ë‹¹
- user:reset:password  ë¹„ë?ë²ˆí˜¸ ?¬ì„¤??
- agent:manage         ?ì´?„íŠ¸ ?„ì²´ ê´€ë¦?
```

#### 2. ì¡°ì§ ê´€ë¦?(org, departmentEntity) - 6ê°?
```
- org:view             ì¡°ì§ ì¡°íšŒ
- org:create           ì¡°ì§ ?ì„±
- org:update           ì¡°ì§ ?˜ì •
- org:move             ì¡°ì§ ?´ë™
- org:delete           ì¡°ì§ ?? œ
- org:manage           ì¡°ì§ ?„ì²´ ê´€ë¦?
```

#### 3. RBAC ê´€ë¦?(rbac, role, permission) - 9ê°?
```
- rbac:view            RBAC ì¡°íšŒ
- rbac:create:role     ??•  ?ì„±
- rbac:update:role     ??•  ?˜ì •
- rbac:delete:role     ??•  ?? œ
- rbac:create:permission ê¶Œí•œ ?ì„±
- rbac:update:permission ê¶Œí•œ ?˜ì •
- rbac:delete:permission ê¶Œí•œ ?? œ
- rbac:assign:permission ê¶Œí•œ ? ë‹¹
- rbac:configure       RBAC ?„ì²´ ?¤ì •
```

#### 4. ë³´ê³ ??ë°?ê°ì‹œ (report, audit, cdr) - 7ê°?
```
- report:view          ë³´ê³ ??ì¡°íšŒ
- report:read          ë³´ê³ ???½ê¸°
- report:export        ë³´ê³ ???´ë³´?´ê¸°
- report:manage        ë³´ê³ ??ê´€ë¦?
- audit:view           ê°ì‚¬ ë¡œê·¸ ì¡°íšŒ
- audit:export         ê°ì‚¬ ë¡œê·¸ ?´ë³´?´ê¸°
- cdr:view             CDR ì¡°íšŒ
```

#### 5. ì±„ë„ ê´€ë¦?(phone, chat, email, queue) - 7ê°?
```
- phone:accept         ?„í™” ?˜ë½
- phone:hold           ?„í™” ë³´ë¥˜
- phone:transfer       ?„í™” ?„í™˜
- chat:send            ì±„íŒ… ?„ì†¡
- chat:receive         ì±„íŒ… ?˜ì‹ 
- email:send           ?´ë©”???„ì†¡
- queue:manage         ??ê´€ë¦?
```

#### 6. ê¸°í? (dashboard, quality) - 2ê°?
```
- dashboard:view       ?€?œë³´??ì¡°íšŒ
- quality:manage       ?ˆì§ˆ ê´€ë¦?
```

---

### ??• (Role) ?•ì˜

#### ??•  ?€??
- **POSITION**: ì¡°ì§??ì§ìœ„ (ADMIN, MANAGER, TEAM_LEAD, MEMBER)
- **CHANNEL**: ?ë‹´ ì±„ë„ (PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR)
- **SKILL**: ê¸°ìˆ /?¤í‚¬ (?¥í›„ ?•ì¥??

#### ê¸°ë³¸ ??•  ë°?ê¶Œí•œ ? ë‹¹

| ??•  | ?€??| ê¶Œí•œ ??| ì£¼ìš” ê¶Œí•œ |
|------|------|---------|-----------|
| **ADMIN** | POSITION | 35ê°?(?„ì²´) | user:*, org:*, rbac:*, report:*, audit:*, ëª¨ë“  ì±„ë„ |
| **MANAGER** | POSITION | 12ê°?| user ?ì„±/?˜ì •, org ?ì„±/?˜ì •/?´ë™, report ?„ì²´ |
| **TEAM_LEAD** | POSITION | 5ê°?| user:read, org:view, report:view/read/export |
| **MEMBER** | POSITION | 4ê°?| user:read:self, user:update:self, org:view, report:view |
| **PHONE_AGENT** | CHANNEL | 3ê°?| phone:accept, phone:hold, phone:transfer |
| **CHAT_AGENT** | CHANNEL | 2ê°?| chat:send, chat:receive |
| **EMAIL_AGENT** | CHANNEL | 1ê°?| email:send |
| **SUPERVISOR** | CHANNEL | 7ê°?| ëª¨ë“  ì±„ë„ + queue:manage |

---

### ê¶Œí•œ ê³„ì¸µ ?ˆì‹œ

```
ADMIN (35ê°?ê¶Œí•œ - ?„ì²´)
?œâ? user:* (9ê°?
?œâ? org:* (6ê°?
?œâ? rbac:* (9ê°?
?œâ? report:* (4ê°?
?œâ? audit:* (2ê°?
?œâ? ì±„ë„ ?„ì²´ (7ê°?
?”â? dashboard, quality (2ê°?

MANAGER (12ê°?ê¶Œí•œ)
?œâ? user: create, read, update, assign:role, reset:password
?œâ? org: view, create, update, move
?”â? report: view, read, export

MEMBER (4ê°?ê¶Œí•œ - ìµœì†Œ)
?œâ? user:read:self
?œâ? user:update:self
?œâ? org:view
?”â? report:view

PHONE_AGENT (3ê°?ê¶Œí•œ)
?œâ? phone:accept
?œâ? phone:hold
?”â? phone:transfer
```

---

## ?¤ê³„ ?ì¹™ ë°??´ìœ 

### 1. UUIDë¡??µì¼???´ìœ 
- ??ë¶„ì‚° ?˜ê²½ ì§€??(ID ì¶©ëŒ ?†ìŒ)
- ??ë©€?°í…Œ?Œì‹œ ?ˆì „??(?Œë„Œ??ê°?ID ì¶©ëŒ ë¶ˆê?)
- ??ë³´ì•ˆ (?œì°¨ ID ?¸ì¶œ ë°©ì?)
- ???¼ê???(ëª¨ë“  ?”í‹°???™ì¼???•ì‹)

### 2. ?ê¸°ì°¸ì¡° FK ?¬ìš© ?´ìœ 
- ??ê³„ì¸µ êµ¬ì¡° ?œí˜„ ìµœì ??
- ??org_pathë¡?ê²½ë¡œ ?ìƒ‰ ë¹ ë¦„
- ??depthë¡??ˆë²¨ ?½ê²Œ ?Œì•…
- ??? ì—°??ë¶€??ì¶”ê?/?œê±°

### 3. ì¤‘ê°„ ?Œì´ë¸??¬ìš© ?´ìœ 
- ??N:M ê´€ê³„ë? ëª…ì‹œ?ìœ¼ë¡?ê´€ë¦?
- ??? ë‹¹ ?¼ì‹œ ??ë©”í??°ì´???€??ê°€??
- ??ê°ì‚¬ ì¶”ì  ?©ì´
- ???±ëŠ¥ ìµœì ??(ì¡°ì¸ ëª…í™•??

### 4. Soft Delete ?¬ìš© ?´ìœ 
- ???ˆìŠ¤? ë¦¬ ? ì?
- ??ê°ì‚¬ ì¶”ì  (?¸ì œ ?´ì§?ˆëŠ”ì§€)
- ???°ì´??ë³µêµ¬ ê°€??
- ??ì°¸ì¡° ë¬´ê²°??? ì?

### 5. ë©€?°í…Œ?Œì‹œ êµ¬í˜„ ?´ìœ 
- ???°ì´??ê²©ë¦¬ (tenant_id ?„ìˆ˜)
- ??SaaS ?•ì¥??
- ??ë³´ì•ˆ (?Œë„Œ??ê°??°ì´???‘ê·¼ ë¶ˆê?)

---

## ë¶€ë¡? ë¹ ë¥¸ ì°¸ì¡°

### ì£¼ìš” ì¿¼ë¦¬ ?¨í„´

#### 1. ?¬ìš©?ì˜ ëª¨ë“  ê¶Œí•œ ì¡°íšŒ
```sql
SELECT DISTINCT p.code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN role_permissions rp ON ar.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.agent_id = :agentId
  AND a.status = 'ACTIVE'
  AND a.tenant_id = :tenantId;
```

#### 2. ë¶€?œì˜ ?„ì²´ ?˜ìœ„ ë¶€??ì¡°íšŒ (?¸ë¦¬)
```sql
SELECT *
FROM departmentEntities
WHERE org_path LIKE CONCAT(:targetOrgPath, '%')
  AND tenant_id = :tenantId
ORDER BY depth, name;
```

#### 3. ??• ??? ë‹¹??ëª¨ë“  ê¶Œí•œ ì¡°íšŒ
```sql
SELECT p.code, p.description
FROM role_permissions rp
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE rp.role_id = :roleId
  AND p.tenant_id = :tenantId;
```

#### 4. ?¬ìš©?ê? ?¹ì • ê¶Œí•œ??ë³´ìœ ?ˆëŠ”ì§€ ?•ì¸
```sql
SELECT EXISTS (
    SELECT 1
    FROM agents a
    JOIN agent_roles ar ON a.agent_id = ar.agent_id
    JOIN role_permissions rp ON ar.role_id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.permission_id
    WHERE a.agent_id = :agentId
      AND p.code = :permissionCode
      AND a.status = 'ACTIVE'
      AND a.tenant_id = :tenantId
) AS has_permission;
```

---

## 6. ?°ì´?°ë² ?´ìŠ¤ ì´ˆê¸°??ë°©ë²•

### ?“Œ ?„ì „ ì´ˆê¸°??(ê¶Œì¥)

**? ï¸ ì£¼ì˜**: ëª¨ë“  ?°ì´?°ê? ?? œ?©ë‹ˆ??

#### ë°©ë²• 1: SQL ?¤í¬ë¦½íŠ¸ ì§ì ‘ ?¤í–‰
```bash
# 1. PostgreSQL ?´ë¼?´ì–¸?¸ì—???¤í–‰
psql -U nexfron -d nexfron -f reset_database_clean.sql

# 2. ? í”Œë¦¬ì??´ì…˜ ?¬ì‹œ??(Flyway ?ë™ ë§ˆì´ê·¸ë ˆ?´ì…˜)
./gradlew bootRun
```

#### ë°©ë²• 2: DBeaver/DataGrip ??GUI ?„êµ¬
1. `reset_database_clean.sql` ?Œì¼ ?´ê¸°
2. ?„ì²´ ? íƒ ???¤í–‰ (Ctrl+Enter)
3. ê²°ê³¼ ?•ì¸: `???°ì´?°ë² ?´ìŠ¤ ?„ì „ ì´ˆê¸°???„ë£Œ!`
4. ? í”Œë¦¬ì??´ì…˜ ?¬ì‹œ??

### ?”„ Flyway ë§ˆì´ê·¸ë ˆ?´ì…˜

? í”Œë¦¬ì??´ì…˜ ?œì‘ ???ë™?¼ë¡œ:
1. `V1_0_0__Complete_Init.sql` ?¤í‚¤ë§??ì„±
2. ?œì? ?°ì´???ë™ ?½ì… (35ê¶Œí•œ + 8??•  + 16?¬ìš©??

### ?“Š ì´ˆê¸°?????•ì¸

```sql
-- ?Œì´ë¸?ëª©ë¡ ?•ì¸
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- ?°ì´??ê±´ìˆ˜ ?•ì¸
SELECT 'departmentEntities' as table_name, COUNT(*) as count FROM departmentEntities
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'agent_roles', COUNT(*) FROM agent_roles;
```

**?ˆìƒ ê²°ê³¼**:
- departmentEntities: 16ê°?
- agents: 16ê°?(admin ?¬í•¨)
- roles: 8ê°?
- permissions: 35ê°?
- role_permissions: 77ê°?
- agent_roles: 22ê°?

---

**ë¬¸ì„œ ?‘ì„±??*: 2026-01-21  
**?‘ì„±??*: Identity System Team  
**ë²„ì „**: 2.0.0 CLEAN  
**?íƒœ**: ìµœì¢… ?¹ì¸ ??
---

> ? ï¸ **ì£¼ì˜?¬í•­**  
> - ëª¨ë“  ?Œì´ë¸”ì? tenant_idë¡?ê²©ë¦¬?˜ì–´???©ë‹ˆ?? 
> - ID??ë°˜ë“œ??UUID (VARCHAR(36)) ?•ì‹???¬ìš©?´ì•¼ ?©ë‹ˆ?? 
> - ?? œ ?•ì±…(ON DELETE)?€ ë°˜ë“œ??ë¬¸ì„œ?€ë¡??¤ì •?´ì•¼ ?©ë‹ˆ?? 
> - ê¶Œí•œ ì½”ë“œ??`domain:action` ?•ì‹???„ê²©??ì¤€?˜í•´???©ë‹ˆ??

