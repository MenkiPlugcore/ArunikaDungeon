# Security report

Threats reviewed: duplicate clicks, stale menus, inventory drag/shift/hotbar/offhand movement, forged ticket metadata, serial replay, ticket refund replay, uncertain crash transactions, negative/corrupt stock, purchase-counter overflow, arena isolation, gateway remote invocation and configuration path traversal.

Exchange GUI validates holder/owner, active view, permission, eligibility and catalog equality again on the scheduled action. One busy player and SQLite pending state block concurrent purchases. A bounded 128-job single-owner worker performs Exchange DB work. Prepared statements bind external transaction values. WAL and synchronous=FULL are used. RESERVED cancellation restores stock/count in one transaction; DEBITED is never automatically refunded.

Ticket states are AVAILABLE → HELD → SPENT → EXPOSED → CONSUMED. Refund creates a new generation only from HELD/SPENT. Old physical copies cannot redeem after ledger advancement. Claims may reproduce a physical serial across a crash window, but serial redemption remains single-use. Ticket storage and player saves cannot be one atomic transaction.

Gateway authorization checks bound UUID, dedicated permanent hidden admin stone, physical world/radius/chunk/anchor, permission and session membership. Normal unbound Waystones are passed through. The adapter uses public methods verified against local CdrWaystone 0.7.0 bytecode; actual renderer/client behavior remains untested.

Tracked loot rejects outsider pickup, hopper pickup and merging. Custom Mythic drops, summons, pets, terrain skills and other plugins' effects are not covered by direct entity tracking. Configure test mobs without independent drops/terrain effects until runtime acceptance.

Unissued loot recovery never automatically replays rewards. Staff must distinguish a record from any partial delivery, inspect session/ticket logs and record compensation once. Do not delete recovery files as a way to retry a drop. Disk-write failure is logged as an error and is not a safe reason to repeat delivery.

No production credentials, database, player inventory, UUID list or world files are included in release assets. Testing IDs are generated UUIDs. Release remains draft/staging, with no claim of production security certification.
