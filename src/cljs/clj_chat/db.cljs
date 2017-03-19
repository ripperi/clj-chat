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
   :login-needed? true})
