(ns todo.core
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.util.response :refer [redirect]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [java-time.api :as jt]))

;; --- Time functions ---

(defn time-now []
  (jt/format "HH:mm" (jt/local-time)))

(defn date-off [days]
  (jt/format "dd-MM-YYYY" (jt/plus (jt/local-date) (jt/days days))))

(defn weekday-off [days]
  (str (jt/day-of-week (jt/plus (jt/local-date) (jt/days days)))))

(def dates (atom ["06-10-2025"
                  "07-10-2025"
                  "08-10-2025"
                  "09-10-2025"
                  "10-10-2025"]))
                                            
;; --- File paths ---
(def tff "current/")
(def aff "archive/")

(def task-file-names
 ["tasks-op1-mon.edn"
  "tasks-op1-tue.edn"
  "tasks-op1-wed.edn"
  "tasks-op1-thu.edn"
  "tasks-op1-fri.edn"
  "tasks-op2-mon.edn"
  "tasks-op2-tue.edn"
  "tasks-op2-wed.edn"
  "tasks-op2-thu.edn"
  "tasks-op2-fri.edn"])
(def users-file "users.edn")

;; --- EDN helpers ---
(defn read-edn [file-path default]
  (if (.exists (io/file file-path))
    (edn/read-string (slurp file-path))
    default))

(defn write-edn [file-path data]
  (spit file-path (pr-str data)))

;; --- Persistent state ---
(def tasks
  (reduce #(conj %1 (atom (read-edn (str tff %2) []))) [] task-file-names))
(def users (atom (read-edn users-file {"admin" "12345"})))
(def current-user (atom nil))
(defonce server (atom nil)) ;; store Jetty server instance

;; --- Check tasks ----

(println "Tasks:")
(doseq [i (range 10)]
  (println (task-file-names i) (count @(tasks i))))

;; --- General Layout Page ---
(defn layout [title & body]
  (page/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:title title]]
    [:body
     [:div {:style "font-family:sans-serif;margin:2em"}
      body]]))

;; --- Preview page ---
(defn preview-page [content]
  (layout "Просмотр"
    [:h1 "Пользователи"]
    [:pre content]
    [:br]
    [:a {:href "/"} " Назад"]))

;; --- Routes ---
(defn home-page []
  (page/html5
   [:head
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "/css/todo.css"}]
    [:script {:src "/js/ui.js?v=20260820-2" :defer true}]
    [:title "Task Planner"]]
   [:body
    [:div {:class "app-toolbar"}
     [:button {:type "button" :data-action "language"} "🇬🇧 English"]
     [:button {:type "button" :data-action "theme"} "🌙 Dark"]]
    [:h1 "Список задач"]
    [:div {:class "search-panel"} [:input {:id "task-search" :type "search" :placeholder "Поиск задач…" :aria-label "Поиск задач"}]]
    (if @current-user
      [:div
       [:p (str "Добро пожаловать, " @current-user "! ")
        [:a {:href "/logout"} "Выход"]]

       ;; Task lists
       [:div
       [:h3 "Команда 1"]
       [:div {:class "container5"}
         [:div
           [:div {:class "weekday"} ;;{:style "height: 240px; width: 380px; overflow: scroll; border: 1px solid #ccc;"}
              "Понедельник " (nth @dates 0)
             [:ul
              (for [task @(tasks 0)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 0)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 0}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"}
              "Вторник " (nth @dates 1)
             [:ul
              (for [task @(tasks 1)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 1)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 1}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"} ;;{:style "height: 240px; width: 380px; overflow: scroll; border: 1px solid #ccc;"}
              "Среда " (nth @dates 2)
             [:ul
              (for [task @(tasks 2)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 2)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 2}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"}
              "Четверг " (nth @dates 3)
             [:ul
              (for [task @(tasks 3)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 3)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 3}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"}
              "Пятница " (nth @dates 4)
             [:ul
              (for [task @(tasks 4)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 4)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 4}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]]]
       [:div
       [:h3 "Команда 2"]
       [:div {:class "container5"}
         [:div
           [:div {:class "weekday"} ;;{:style "height: 240px; width: 380px; overflow: scroll; border: 1px solid #ccc;"}
              "Понедельник " (nth @dates 0)
             [:ul
              (for [task @(tasks 5)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 5)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 5}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"}
              "Вторник " (nth @dates 1)
             [:ul
              (for [task @(tasks 6)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 6)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 6}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"} ;;{:style "height: 240px; width: 380px; overflow: scroll; border: 1px solid #ccc;"}
              "Среда " (nth @dates 2)
             [:ul
              (for [task @(tasks 7)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 7)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 7}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"}
              "Четверг " (nth @dates 3)
             [:ul
              (for [task @(tasks 8)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 8)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 8}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]
         [:div
           [:div {:class "weekday"}
              "Пятница " (nth @dates 4)
             [:ul
              (for [task @(tasks 9)]
                [:li (str task)
                 (when (= @current-user "admin")
                   [:a {:href (str "/remove-task/" task "/" 9)} " Удалить"])])]]
           ;; Add task form
           (form/form-to [:post "/add-task"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:input {:type "hidden" :name "file-idx" :value 9}]
                         [:input {:type "text" :name "task" :placeholder "Новая задача" :required true}]
                         [:button "Добавить задачу"])]]]

       ;; Begin new week, add user and stop server forms (admin only)
       (when (= @current-user "admin")
         [:div {:class "container5"}
           [:div
            [:h3 "Начать новую неделю"]
            (form/form-to [:post "/new-week"]
                          [:input {:type "hidden" :name "__anti-forgery-token"
                                   :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                          [:input {:type "checkbox" :name "clear-tasks"} "Очистить списки задач"]
                          [:br]
                          [:input {:type "checkbox" :name "arch-tasks"} "Архивировать списки задач"]
                          [:br]
                          [:button "Начать"])]
           [:div
            [:h3 "Добавить нового пользователя"]
            (form/form-to [:post "/add-user"]
                          [:input {:type "hidden" :name "__anti-forgery-token"
                                   :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                          [:input {:type "text" :name "username" :placeholder "Имя пользователя" :required true}]
                          [:input {:type "password" :name "password" :placeholder "Пароль" :required true}]
                          [:button "Добавить пользователя"])]

           [:div
            [:h3 "Список пользователей"]
            (form/form-to [:post "/user-list"]
                          [:input {:type "hidden" :name "__anti-forgery-token"
                                   :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                          [:button "Показать"])]
           [:div]
           [:div
           [:h3 "Остановить сервер"]
           (form/form-to [:post "/shutdown"]
                         [:input {:type "hidden" :name "__anti-forgery-token"
                                  :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                         [:button "Остановить"])]])]

      ;; Not logged in
      [:a {:href "/login"} "Вход"])]))

;; --- Handlers ---
(defn add-task [task file-idx]
  (let [task (str (time-now) " " @current-user ": " task)
        idx (read-string file-idx)]
    (swap! (tasks idx) #(let [new-tasks (conj % task)]
                  (write-edn (str tff (task-file-names idx)) new-tasks)
                  new-tasks)))
  (redirect "/"))

(defn remove-task [task file-idx]
  (when (= @current-user "admin")
    (let [idx (read-string file-idx)]
      (swap! (tasks idx) #(let [new-tasks (vec (remove (fn [t] (= t task)) %))]
                     (write-edn (str tff (task-file-names idx)) new-tasks)
                     new-tasks))))
  (redirect "/"))

(defn add-user [username password]
  (when (= @current-user "admin")
    (swap! users #(let [new-users (assoc % username password)]
                     (write-edn users-file new-users)
                     new-users)))
  (redirect "/"))

(defn shutdown-server []
  (when @server
    (.stop @server)
    (reset! server nil)))

(defn login-page []
  (page/html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "/css/todo.css"}]
    [:script {:src "/js/ui.js?v=20260820-2" :defer true}]
    [:title "Вход"]]
   [:body {:class "login-page"}
    [:div {:class "app-toolbar"}
     [:button {:type "button" :data-action "language"} "🇷🇺 Русский"]
     [:button {:type "button" :data-action "theme"} "🌙 Dark"]]
    [:div {:class "login-card"}
     [:h1 "Вход"]
     (form/form-to [:post "/login"]
                   [:input {:type "hidden" :name "__anti-forgery-token"
                            :value ring.middleware.anti-forgery/*anti-forgery-token*}]
                   [:input {:type "text" :name "username" :placeholder "Имя пользователя" :required true}]
                   [:input {:type "password" :name "password" :placeholder "Пароль" :required true}]
                   [:button "Войти"])]]))

(defn login [username password]
  (if (= (get @users username) password)
    (do
      (reset! current-user username)
      (redirect "/"))
    (home-page)))

(defn logout []
  (reset! current-user nil)
  (redirect "/"))
  
(defn fill-dates []
  (reset! dates [])
  (doseq [i (range 8)]
    (if (= (weekday-off i) "MONDAY")
      (doseq [j (range 5)]
        (swap! dates conj 
          (date-off (+ i j)))))))
          
(defn clear-all-tasks []
  (doseq [i (range 10)]
    (write-edn (str tff (task-file-names i)) [])))
          
(defn archive-all-tasks []
  (let [fin (str "tasks_" (nth @dates 0) "_" (nth @dates 4) ".txt")
        ftt (fn [k]
              (str (nth @dates k) " Команда1\n\n"
                (apply str (interpose "\n" @(tasks k))) "\n\n"
                (nth @dates k) " Команда2\n\n" 
                (apply str (interpose "\n" @(tasks (+ k 5)))) "\n\n"))
        stt (str (ftt 0)(ftt 1)(ftt 2)(ftt 3)(ftt 4))]
    (spit (str aff fin) stt)))

(defn new-week [clear-tasks arch-tasks]
  (if clear-tasks
    (clear-all-tasks))
  (if arch-tasks
    (archive-all-tasks))
  (fill-dates)
  (redirect "/"))

(defn map->html-table [mp hk hv]
  (let [header (str "<table style='border: 1px solid black;'><tr><th>" hk "</th><th>" hv "</th></tr>")
        frow (fn [k v] (str "<tr><td>" k "</td><td>" v "</td></tr>"))
        footer "</table>"
        data (for [[k v] mp] (frow k v))]
    (str header (apply str data) footer)))

(defn user-list []
  (let [ul (read-edn users-file [])
        tbl (map->html-table ul "Логин" "Пароль")]
    (preview-page tbl)))

;; --- Routes ---
(defroutes app-routes
  (GET "/" [] (home-page))
  (POST "/add-task" [task file-idx] (add-task task file-idx))
  (GET "/remove-task/:task/:file-idx" [task file-idx] (remove-task task file-idx))
  (POST "/new-week" [clear-tasks arch-tasks] (new-week clear-tasks arch-tasks))
  (POST "/add-user" [username password] (add-user username password))
  (POST "/user-list" [] (user-list))
  (POST "/shutdown" [] (do (shutdown-server) (redirect "/")))
  (GET "/login" [] (login-page))
  (POST "/login" [username password] (login username password))
  (GET "/logout" [] (logout))
  (route/resources "/")
  (route/not-found "Страница не найдена!"))

;; --- Main ---
(defn -main []
  (let [port 8888]
    (reset! server (jetty/run-jetty
                    (wrap-defaults (wrap-anti-forgery app-routes) site-defaults)
                    {:host "0.0.0.0" :port port :join? false}))
    (println "To-do server started on port:" port)))
