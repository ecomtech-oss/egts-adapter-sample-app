## Egts-adapter-sample-app
### Описание
Демо приложение, для демонстрации использования библиотеки и Spring-boot стартера egts-adapter
в Spring-boot проекте.
Приложение позволяет установить соединение с запущенным моком tcp сервера РНИС и осуществлять
обмен пакетами - авторизацию, отправку телематических пакетов и прием ответов

### Запуск сервиса
Для подключения адаптера в проект необходимо добавить зависимость
tech.ecom.courier-passport.egts:egts-adapter:<version> в build.gradle.kts проекта

если нужен готовый спринг бин нужно добавить проперти
egts:
    initialize-encoders: true
после этого в классы клиента можно инжектить бин egtsPacketEncoder: EgtsPacketEncoder
примеры использования бина и сборки дата классов пакета можно посмотрет в юнит тесте
модуля library

### Конфигурация
отдельной конфигурации не требуется