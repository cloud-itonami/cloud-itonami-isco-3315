(ns valuation.store
  "SSoT for the ISCO-08 3315 independent valuation and loss assessment
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a property-condition documentation
  robot performs damage photographing, assessment-report assembly and physical
  filing under this advisor/governor pair, which never dispatches hardware
  itself and never issues a valuation above the client's registered
  assessment-authority ceiling). Modeled on
  cloud-itonami-isco-3314's statanalysis.store.

  Domain:

    client  — a registered property owner/insurance company
              (:client-id, :name, :assessment-authority-ceiling)
    asset   — a registered property for valuation {:asset-id :client-id
              :property-type :inspection-completed?}. `:inspection-completed?`
              is whether a site inspection has been performed — issuing an
              assessment without inspection evidence is incomplete due diligence,
              not professional valuation service.
    record  — a committed operating record (an issued valuation or
              finalized assessment) — written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (asset [s asset-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-asset! [s a])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (asset [_ asset-id] (get-in @a [:assets asset-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-asset! [s asset]
    (swap! a assoc-in [:assets (:asset-id asset)] asset) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :assets {} :records [] :ledger []}
                                   seed)))))
