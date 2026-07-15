(ns valuation.advisor
  "Valuation Advisor — the advisor named in this repository's README,
  proposing a valuation or assessment operation (finalize assessment,
  issue valuation) from a client asset, inspection results and policy terms.
  Swappable mock/llm; the advisor ONLY proposes — `valuation.governor`
  checks the site inspection requirement and assessment-authority ceiling
  independently and always escalates finalize-assessment and issue-valuation
  decisions. Modeled on cloud-itonami-isco-3314's statanalysis.advisor.

  A proposal: {:op :finalize-assessment|:issue-valuation
               :effect :propose :asset-id str :valuation-amount number
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake asset-id valuation-amount] :as request}]
  {:op op
   :effect :propose
   :asset-id asset-id
   :valuation-amount (or valuation-amount 0)
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a property valuation advisor. Given a request, propose an
   :op, the :asset-id, :valuation-amount, an honest :confidence and a :stake.
   Never propose a valuation above the client's registered assessment-authority
   ceiling — the governor checks it against the registered client record.
   Finalize-assessment and issue-valuation always require human sign-off
   regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "valuation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
