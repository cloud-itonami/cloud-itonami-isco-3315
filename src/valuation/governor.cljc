(ns valuation.governor
  "ValuationAssessmentGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every valuation issuance and assessment finalization an advisor may
  propose for a property. The governor never dispatches hardware itself
  and never issues a valuation above the client's registered
  assessment-authority ceiling. Modeled on
  cloud-itonami-isco-3314's statanalysis.governor.
  Task twist: a valuation amount is an arithmetic ceiling
  against the client's registered assessment-authority ceiling, and a
  valuation cannot be issued until the property asset has been inspected.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the property owner/insurance company
                                must be registered.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never issues a valuation above the
                                registered assessment-authority ceiling;
                                it only gates what the advisor may
                                publish).
    3. asset basis            — a valuation proposal must cite a
                                REGISTERED asset belonging to this
                                client.
    4. asset-wrong-client     — an asset must belong to the request's
                                client.
    5. site-inspection-required — the asset must have completed site
                                inspection before any valuation can be
                                finalized or issued (issuing valuation
                                without inspection evidence is incomplete
                                due diligence, not professional service).
    6. valuation-amount-exceeds-ceiling — the valuation amount must not
                                exceed the client's registered
                                assessment-authority ceiling (issuing
                                beyond the client's registered ceiling
                                is unauthorized issuance, not routine
                                valuation service).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    7. :op :finalize-assessment (assessment finalization always requires
                                human sign-off per Trust Control 2).
    8. :op :issue-valuation (valuation issuance always requires human
                            sign-off per Trust Control 1).
    9. low confidence (< `confidence-floor`)."
  (:require [valuation.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:finalize-assessment
                                     :issue-valuation})

(defn- hard-violations [{:keys [request proposal]} client-record asset]
  (let [{:keys [op valuation-amount]} proposal]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は登録権限上限超過の発行を直接実行しない）"})

      (nil? asset)
      (conj {:rule :unknown-asset :detail "未登録 asset への査定提案は不可"})

      (and asset (not= (:client-id asset) (:client-id request)))
      (conj {:rule :asset-wrong-client :detail "asset が別 client のもの"})

      (and asset (not (:inspection-completed? asset)))
      (conj {:rule :site-inspection-required
             :detail "検査完了前の査定金額発行は不完全なデューデリジェンスであって適切なサービスではない"})

      (and client-record asset (number? valuation-amount)
           (> valuation-amount (:assessment-authority-ceiling client-record)))
      (conj {:rule :valuation-amount-exceeds-ceiling
             :detail (str "査定金額 " valuation-amount " > 登録済み上限 "
                          (:assessment-authority-ceiling client-record) "（登録上限を超える発行は無許可発行であって通常の査定業務ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `valuation.store/Store`. Pure — never
  mutates the store, never issues a valuation above the registered
  assessment-authority ceiling."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        asset (some->> (:asset-id proposal) (store/asset store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record asset)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
