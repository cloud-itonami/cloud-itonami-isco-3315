(ns valuation.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [valuation.actor :as actor]
            [valuation.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Insurance Company"
                                :assessment-authority-ceiling 500000})
    (store/register-asset! st {:asset-id "A-1" :client-id "client-1"
                               :property-type "residential"
                               :inspection-completed? true})
    st))

(deftest commits-a-within-ceiling-inspected-valuation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :assess-damage :stake :low
                 :asset-id "A-1" :valuation-amount 250000}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-above-ceiling-valuation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :issue-valuation :stake :low
                 :asset-id "A-1" :valuation-amount 750000}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-finalize-assessment-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :finalize-assessment :stake :low
                 :asset-id "A-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
