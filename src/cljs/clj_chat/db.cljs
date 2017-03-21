(ns clj-chat.db)

(def default-db
  {:name "clj-chat"
   :groups []
   :group nil
   :members []
   :member nil
   :messages []
   :direct-messages []
   :channel nil
   :background-dim false
   :add-group false
   :add-channel false
   :add-member false
   :login-needed? true})
