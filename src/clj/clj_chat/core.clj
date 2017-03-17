(ns clj-chat.core)

;; ----------atoms to keep track of rooms and users----------

(defonce rooms_ (atom {}))

(defonce users_ (atom {}))

;; ----------functions for managing said atoms----------

(defn add-room!
  ([name]
   (add-room! name nil))
  ([name creator-id]
   (swap! rooms_ assoc (keyword name) {:id name :name name :users {} :channels ["#general"] :owner creator-id})))

(defn add-channel! [room channel]
  (swap! rooms_ update-in [(keyword room) :channels] conj channel))

(defn add-user!
  ([id]
   (add-user! id nil []))
  ([id name room-keys]
   (swap! users_ assoc (keyword id) {:id id :name name :rooms room-keys})))

(defn add-to-room! [room user]
  (swap! rooms_ update-in [(keyword room) :users] assoc (keyword user) {:id user :name nil})
  (swap! users_ update-in [(keyword user) :rooms] conj room))

(defn remove-from-room! [room user]
  (swap! rooms_ update-in [(keyword room) :users] dissoc (keyword user))
  (swap! users_ update-in [(keyword user) :rooms] #(vec (remove #{room} %))))

(defn remove-user-from-rooms! [user]
  (doseq [room (:rooms ((keyword user) @users_))]
    (remove-from-room! room user)))

(defn remove-user! [user]
  (swap! users_ dissoc (keyword user)))

(defn update-username-for-rooms! [user username]
  (let [users-rooms (get-in @users_ [(keyword user) :rooms])]
    (doseq [room users-rooms]
      (swap! rooms_ assoc-in [(keyword room) :users (keyword user) :name] username))))

(defn set-username! [user username]
  (swap! users_ assoc-in [(keyword user) :name] username)
  (update-username-for-rooms! user username))

(defn get-rooms-with-keys [user]
  (select-keys @rooms_ (map keyword (get-in @users_ [(keyword user) :rooms]))))

(defn get-rooms [user]
  (map #((keyword %) @rooms_) (:rooms ((keyword user) @users_))))

(defn get-users-in-room [room]
  (map :id (vals (get-in @rooms_ [(keyword room) :users]))))

(defn get-neighbouring-users [user]
  (distinct (reduce concat (map
                            #(remove
                              #{user}
                              (get-users-in-room %))
                            (get-in @users_ [(keyword user) :rooms])))))

(defn login-needed? [user]
  (nil? (:name ((keyword user) @users_))))

(defn valid-message? [msg]
  (not (or (nil? (:channel msg)) (nil? (:group msg)))))

(defn allowed-message? [uuid msg]
  (let [user ((keyword uuid) @users_)
        users-rooms (:rooms user)
        msg-room (:group msg)
        msg-channel (:channel msg)
        room-channels (:channels ((keyword msg-room) @rooms_))]
    (and (some #(= msg-room %) users-rooms)
         (some #(= msg-channel %) room-channels))))

(defn room-name-free? [room]
  (not (contains? @rooms_ (keyword room))))
