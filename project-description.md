# Inventory Management App — Product Description

## What this app is

A multi-tenant inventory management system. Users create "Spaces" (think of
each Space as a separate inventory — e.g. a warehouse, a shop, a home
pantry), track Products with stock levels inside each Space, invite other
people to collaborate on a Space with different permission levels, and see
dashboards/analytics/audit history for their inventory activity.

It's a B2B/prosumer-style tool — the primary user is someone managing
physical stock (small business owner, warehouse manager, or a household
tracking supplies) who needs visibility into what they have, what's running
low, and who changed what.

---

## 1. Authentication & Onboarding

### 1.1 Sign up (email + password)
- New user provides **name, email, password**.
- Account is created but **unverified** — user cannot fully use the app yet.
- An OTP (one-time numeric code) is emailed to them.
- User enters the OTP to verify their email. Once verified, they can log in.
- If the OTP expires or is lost, user can request a **resend**.

### 1.2 Sign up / Sign in via Google
- User clicks **"Sign in with Google"**, goes through Google's standard
  consent screen, and is returned to the app already authenticated —
  no OTP step, no password ever set.
- If this is the person's first time (no account with that email exists
  yet), an account is silently created for them, already verified.
- If an account with that email already exists (e.g. they'd previously
  signed up with email/password), Google sign-in logs into that same
  existing account rather than creating a duplicate — same person, same
  data, just a second way in.
- A user who signed up via Google has **no password** and cannot use the
  email/password login form — Google is their only way in unless they
  separately set a password later (not currently supported, but worth
  designing the login screen to not be confusing for this case — e.g. no
  "forgot password" trap for Google-only users).

### 1.3 Log in (email + password)
- Standard email + password form.
- Fails uniformly ("Invalid credentials") whether the email doesn't exist
  or the password is wrong — the UI should not hint which one was wrong,
  for security reasons.
- On success, returns a session token used for every subsequent action.

### 1.4 Session / logged-in state
- Once logged in (either method), the user holds a token that represents
  their identity for as long as it's valid (~24 hours currently). There's
  no separate "remember me" distinction yet — treat the session as
  present/absent.
- On logout or token expiry, user is returned to the sign-in screen.

**Design implication:** the sign-in screen needs two clearly distinct
paths — a traditional email/password form, and a prominent "Sign in with
Google" button — presented as equally valid, not one as a fallback for
the other.

---

## 2. Spaces (the core organizing concept)

A **Space** is a named inventory container. Every Product belongs to
exactly one Space. A user can own multiple Spaces and can also be a member
of Spaces owned by other people.

### 2.1 First-run / empty state
- A brand-new user has **no Spaces**. The very first meaningful action in
  the app is **"Create a Space"** — this should be the dominant call to
  action on an empty dashboard, not a buried menu item.
- There's a "space creation status" check the frontend can poll/read to
  know whether a Space is mid-creation/still initializing (useful for a
  loading state right after the create action).

### 2.2 Creating a Space
- User provides a **name** for the Space (e.g. "Downtown Warehouse",
  "Home Pantry").
- The creator automatically becomes that Space's **Owner**.
- A user cannot create two Spaces with the exact same name (enforced
  uniquely per-owner) — the UI should surface that as a friendly
  validation error, not a generic failure.

### 2.3 Viewing Spaces
- A user's Spaces split into two lists:
  - **Owned Spaces** — Spaces they created and fully control.
  - **Shared Spaces** — Spaces owned by someone else that they've been
    invited into and accepted.
- A natural UI pattern: a Space switcher/selector (sidebar or top-level
  dropdown) showing both groups, since most of the app (products,
  dashboard, audit log) is scoped to "whichever Space is currently
  selected."

### 2.4 Managing a Space
- Owner can **rename** a Space.
- Owner can **delete** a Space — this should have a strong confirmation
  step (irreversible, wipes an entire inventory and its members).
- Only the Owner can delete a Space; Admins cannot.

---

## 3. Roles & Permissions (inside a Space)

Every person attached to a Space has exactly one role, which changes what
they can see/do **within that specific Space** (roles are per-Space, not
global — someone can be an Owner of one Space and a Viewer on another).

| Role    | Can do |
|---------|--------|
| **Owner**  | Everything: manage products, manage members, invite anyone at any role, rename/delete the Space. Exactly one Owner per Space (the creator). |
| **Admin**  | Manage products (add/edit/delete/stock), invite/manage members — but **can only invite new people as Member or Viewer**, never as Admin or Owner (that's Owner-only, to prevent an Admin silently promoting someone above themselves). Cannot delete the Space. |
| **Member** | Can view and edit products — adjust stock, prices, etc. Cannot manage other members or Space settings. |
| **Viewer** | Read-only. Can see products and stock levels but cannot change anything. |
| **Pending**| Internal-only status — someone who's been invited but hasn't accepted yet. Not a "real" active role; shown as a pending invite, not as a member with permissions. |

**Design implication:** anywhere the UI shows "invite a member," the role
picker should adapt to the current user's own role — an Admin should not
even see Owner/Admin as selectable options; only the Owner sees the full
role list.

### 3.1 Inviting someone
- Owner/Admin enters an email + picks a role.
- The invited person appears as **Pending** until they accept.
- Invited person sees their pending invites somewhere in their own
  account (e.g. a notifications area or a dedicated "Invites" screen) and
  can **Accept** or **Decline**.

### 3.2 Managing members
- Owner/Admin can view the full member list of a Space (with roles).
- Owner/Admin can remove a member from a Space.

---

## 4. Products & Stock

Products are the actual inventory items, always scoped to one Space.

### 4.1 Product fields
- **Name**
- **Price**
- **Current stock** (a count)
- **Minimum quantity** — the low-stock threshold
- **Maximum quantity** — a cap; stock additions that would exceed this are
  rejected with a clear error, so the UI should show this as a hard limit,
  not just a suggestion

### 4.2 Creating / editing / deleting products
- Standard CRUD, scoped to the currently selected Space.
- Requires at least Member-level access to create/edit; Viewers see
  everything read-only.

### 4.3 Stock operations
- **Add stock** — increases current stock by an amount. Blocked if it
  would push stock above the product's maximum quantity (should surface
  a specific error like "Cannot exceed maximum quantity: current 18,
  adding 5 would exceed max of 20").
- **Remove stock** — decreases current stock by an amount. Similarly
  guarded against going negative/below a floor.

### 4.4 Browsing products
- Product list supports **search, pagination, and sorting** (by name,
  price, stock, etc., ascending/descending) — this should be a proper
  data-table/list view, not a simple flat list, once a Space has more
  than a handful of products.
- A dedicated **"Low stock" view** — products at or below their minimum
  quantity, surfaced as a filtered list and also referenced from the
  dashboard (see below) as an alert.

---

## 5. Dashboard & Insights

A dashboard scoped to the currently selected Space (or possibly an
aggregate view — check with the product owner on whether this is
per-Space or cross-Space) showing:

- **Overview** — high-level snapshot (total products, total stock value,
  etc.)
- **Inventory insights** — deeper analytical view of the inventory's
  health
- **Low-stock alerts** — a widget/section surfacing products under their
  minimum threshold, likely linking through to the full low-stock list
- **Recent activity** — a feed of recent changes (ties into the audit
  log, see below)
- **Space metrics** — stats about the Space itself (member count,
  product count, etc.)
- **Top products** — a ranked list (configurable limit, sortable by a
  metric like value)
- **Trends over time** — a time-series view (configurable day range,
  e.g. last 30 days) showing how inventory has changed

**Design implication:** this is the natural "home screen" once a Space is
selected — a grid of cards/widgets, with the low-stock alert and recent
activity being the two most actionable/urgent pieces of information to
put front-and-center.

---

## 6. Audit Log

Every meaningful action (creating/updating/deleting a product, stock
add/remove, member changes, etc.) is recorded as an audit log entry,
including:
- who did it (the actor)
- what kind of entity was affected (Space, Product, User)
- what operation happened (CREATE, UPDATE, DELETE, STOCK_ADD,
  STOCK_REMOVE)
- when it happened
- optional structured details about what changed

The audit log view supports:
- **Filtering** by entity type, operation, entity ID, and a date range
- **Summary view** — an aggregated count/breakdown, useful for a small
  stats widget
- **Recent activity** (last N hours) — a lightweight feed
- **Activity trends** (last N days) — a time-series view of how much
  activity has been happening
- **Filter options** — the UI can fetch what filter values are actually
  available (populates dropdowns dynamically rather than hardcoding)

**Important nuance for design:** audit logs are currently scoped to
**what the logged-in user themselves did**, not a full "everyone's
activity in this Space" feed. Design the audit log screen with that
framing in mind (e.g. "Your recent activity" rather than implying it's a
complete team activity log) unless this changes.

---

## 7. Suggested high-level navigation / IA

A reasonable structure for a frontend design tool to work from:

```
Sign In / Sign Up (Google or email+password)
   └── (first time) Create your first Space
        └── Space Switcher (Owned / Shared)
             └── Dashboard (selected Space)
             ├── Products (list, search, low-stock filter, add/edit, stock in/out)
             ├── Members (list, invite, roles, remove)
             ├── Audit Log (filterable activity history)
             └── Space Settings (rename, delete — Owner only)
   └── My Invites (pending Space invitations awaiting Accept/Decline)
   └── My Account (profile info, currently logged-in-via indicator)
```

---

## 8. Key states and edge cases

- **Empty state**: brand-new user, zero Spaces — strong CTA to create one.
- **Unverified email**: user has registered but not yet completed OTP
  verification — should block full app access with a clear "verify your
  email" warning.
- **Pending invite**: user has been invited to a Space but hasn't
  accepted — shown distinctly from active memberships.
- **Role-gated UI**: Viewers should never see edit/delete affordances;
  Admins should never see an option to invite someone as Owner/Admin;
  only Owners see "Delete Space."
- **Stock limit errors**: add-stock and remove-stock actions can fail
  with a specific, human-readable reason (over max, below min/zero) —
  these should surface as inline validation, not generic error toasts.
- **Low-stock warning**: products at/under their minimum should be
  visually flagged wherever they appear (product list, dashboard).

---

### **Note: Currently we allow free users to have only 10 spaces (both Owned and Shared), to increase subscribe to a premium plan.**

