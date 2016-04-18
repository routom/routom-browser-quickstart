(ns routom-browser-quickstart.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [routom.core :as r]
            [routom.bidi :as rb]
            [bidi.router :as br]
            [om.dom :as dom]))

(enable-console-print!)

(declare set-location!)

(defui Login
  static r/IRootQuery
  (root-query [_] [{:auth [:token :username]}])
  static om/IQuery
  (query [this] [:login/title])
  Object
  (render [this]
    (dom/div
      nil
      (dom/label nil "Username")
      (dom/input #js {:type "text"})
      (dom/label nil "Password")
      (dom/input #js {:type "password"})
      (dom/button #js {:onClick #(set-location! :home {})} "Submit"))))

(defui NavBar
  static om/IQuery
  (query [_] [:nav-bar/title])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [nav-bar/title]} props]
      (dom/div nil
        (dom/h1 nil title)
        (dom/button
          #js {:onClick #(set-location! :home {})}
          "Home")
        (dom/button
          #js {:onClick #(set-location! :about {})}
          "About")
        (dom/button
          #js {:onClick #(set-location! :user {:user/id "1"})}
          "User")
        (om/children this)))))

(defui About
  static om/IQuery
  (query [_] [:about/content])
  Object
  (render [this]
    (let [{:keys [about/content]} (om/props this)]
      (dom/div nil content))))

(defui Home
  static om/IQuery
  (query [_] [:home/content])
  Object
  (render [this]
    (let [{:keys [home/content]} (om/props this)]
      (dom/div nil content))))

(defui User
  static om/IQueryParams
  (params [_] {:user/id nil})
  static r/IRootQuery
  (root-query [_] '[{(:users/by-id {:user/id ?user/id}) [:user/id :user/name]}])
  static om/IQuery
  (query [_] [:user/title])
  Object
  (render [this]

    (let [{:keys [user/title] :as props} (om/props this)
          {:keys [user/name]} (om/get-computed this :users/by-id)]
      (println title)
      (dom/div nil
               (dom/h1 nil title)
               (dom/div nil name)))))

(def routes
  (atom
    {:login {:ui Login
             :bidi/path "/login"}
     :nav-bar
      {:ui NavBar
       :sub-routes
        {:home {:ui Home
                :bidi/path "/"}
         :about {:ui About
                 :bidi/path "/about"}
         :user {:ui User
                :bidi/path ["/users/" :user/id]}}}}))

(def router (r/init-router routes))
(def set-route! (:set-route! router))
(def AppRoot (:root-class router))


(def bidi-routes (rb/routes->bidi-route @routes))
(def bidi-router (br/start-router!
                   bidi-routes
                   {:default-location {:handler :home :route-params {}}
                    :on-navigate #(set-route! {:route/id (:handler %) :route/params (:route-params %)})}))

(defn set-location!
  [route-id route-params]
  (br/set-location! bidi-router {:handler route-id :route-params route-params}))

(def app-state
  (atom
    {:nav-bar
      {:nav-bar/title "Routom Browser Quickstart"}
      :about
      {:about/content "This is the about page"}
      :home
      {:home/content "This is the home page"}
      :login
      {:login/title "Sign in here"}
      :user
      {:user/title "User details"}
      :users/by-id
      {"1" {:user/id 1 :user/name "User1"}}}))

(defmulti read om/dispatch)

(defmethod read :users/by-id
  [{:keys [state]} key params]
  {:value (get-in @state [key (:user/id params)])})

(defmethod read :default
  [{:keys [state]} key _]
  {:value (get @state key)})

(def reconciler
  (om/reconciler
    {:state app-state
      :parser (om/parser {:read read})}))


(om/add-root! reconciler AppRoot (gdom/getElement "app"))
