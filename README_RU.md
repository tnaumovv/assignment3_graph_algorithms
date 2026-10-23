# Assignment 3: алгоритмы на графах

Решение на Java 17. Основной код находится в `src/Java/Algorithms.java`.
Реализованы DSU, Kruskal, Prim, Kosaraju SCC, топологическая сортировка через DFS,
кратчайшие пути в DAG и восстановление маршрута.

Из корня репозитория:

```sh
python3 tools/run.py --setup
python3 tools/run.py --check
python3 tests/check.py --all
python3 tests/benchmark.py
```

Нужны JDK 17+ и Python 3.12+. На Windows вместо `python3` можно использовать `py -3`.

Английский анализ и замеры находятся в [ANALYSIS.md](ANALYSIS.md).
Входы, ожидаемые ответы, объяснения и фактические результаты находятся в [tests/](tests/).
Подробные команды есть в [README.md](README.md).

Для сдачи вставьте ссылку на конкретный коммит в Moodle Online text и нажмите
**Submit assignment**. Срок и отдельный quiz определяются курсом.
