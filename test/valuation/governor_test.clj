(ns valuation.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [valuation.store :as store]
            [valuation.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Insurance Company"
                                :assessment-authority-ceiling 500000})
    (store/register-asset! st {:asset-id "A-1" :client-id "client-1"
                               :property-type "residential"
                               :inspection-completed? true})
    st))

(defn- valuation-op [op confidence valuation-amount]
  {:op op :effect :propose :asset-id "A-1"
   :valuation-amount valuation-amount
   :confidence confidence :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-ceiling-and-inspected
  (let [st (fresh-store)
        v (governor/check req {} (valuation-op :assess-damage 0.9 250000) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceiling-boundary
  (testing "the assessment-authority-ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (valuation-op :assess-damage 0.9 500000) st)]
      (is (:ok? v)))))

(deftest hard-on-valuation-exceeds-ceiling
  (testing "issuing a valuation above the client's registered assessment-authority ceiling is unauthorized issuance, not routine valuation"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (valuation-op :assess-damage 0.99 750000) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :valuation-amount-exceeds-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-inspection-not-completed
  (testing "issuing a valuation without site inspection evidence is incomplete due diligence, not professional service"
    (let [st (fresh-store)
          _ (store/register-asset! st {:asset-id "A-uninspected" :client-id "client-1"
                                       :property-type "commercial"
                                       :inspection-completed? false})
          v (governor/check req {} (assoc (valuation-op :assess-damage 0.99 250000) :asset-id "A-uninspected") st)]
      (is (:hard? v))
      (is (some #(= :site-inspection-required (:rule %)) (:violations v))))))

(deftest hard-on-unknown-asset
  (let [st (fresh-store)
        v (governor/check req {} (assoc (valuation-op :assess-damage 0.9 250000) :asset-id "A-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-asset (:rule %)) (:violations v)))))

(deftest hard-on-foreign-asset
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other Company"
                                :assessment-authority-ceiling 300000})
    (store/register-asset! st {:asset-id "A-2" :client-id "client-2"
                               :property-type "industrial"
                               :inspection-completed? true})
    (let [v (governor/check {:client-id "client-2"} {} (assoc (valuation-op :assess-damage 0.9 250000) :asset-id "A-1") st)]
      (is (:hard? v))
      (is (some #(= :asset-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (valuation-op :assess-damage 0.9 250000) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (valuation-op :assess-damage 0.9 250000) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-finalize-assessment-even-at-high-confidence
  (testing "assessment finalization always requires human sign-off per Trust Control 2"
    (let [st (fresh-store)
          v (governor/check req {} {:op :finalize-assessment :effect :propose
                                    :asset-id "A-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-issue-valuation-even-at-high-confidence
  (testing "valuation issuance always requires human sign-off per Trust Control 1"
    (let [st (fresh-store)
          v (governor/check req {} {:op :issue-valuation :effect :propose
                                    :asset-id "A-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (valuation-op :assess-damage 0.3 250000) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
