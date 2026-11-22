# Bendrabučių skalbyklų rezervavimo sistema

Galutinė III kursinio darbo dalis. Sistema aptarnauja du bendrabučius: gyventojas mato savo bendrabučio mašinų laisvus laikus, sukuria rezervaciją, peržiūri ir atšaukia savo būsimas rezervacijas. Administratorius keičia savo bendrabučio rezervacijos trukmę ir kuria gyventojų paskyras. Sistemos sandara, ryšys su I etapu ir kokybės atributo bandymas pateikti [architektūros dokumente](III_etapas_architektura.md).

## Etapų dokumentai

- [I etapo projektavimo dokumentas](I_etapas_projektavimo_dokumentas.md).
- [II etapo prototipo aprašas (istorinis)](II_etapas_README.md).
- [III etapo architektūros dokumentas](III_etapas_architektura.md).

## Paleidimas

Reikia veikiančio Docker Desktop su Docker Compose. Java, Maven ir PostgreSQL atskirai įdiegti nereikia.

1. Atsidarykite terminalą projekto aplanke.
2. Paleiskite vieną komandą: `docker compose up --build -d`.
3. Atidarykite `http://localhost:8080`. Prisijunkite puslapio formoje su viena iš žemiau pateiktų paskyrų.

| Prisijungimo vardas | Slaptažodis | Teisės |
| --- | --- | --- |
| `student_1_101` | `student_demo` | A bendrabučio gyventojas |
| `student_2_101` | `student_demo` | B bendrabučio gyventojas |
| `admin_1` | `admin_demo` | A bendrabučio administratorius |
| `admin_2` | `admin_demo` | B bendrabučio administratorius |

Abiejuose bendrabučiuose iš pradžių yra mašinos Nr. 2 ir 3. Naują gyventojo paskyrą savo bendrabutyje sukuria administratorius. Norėdami pakeisti paskyrą, paspauskite **Atsijungti / pakeisti paskyrą** ir prisijunkite kitu vardu tame pačiame lange. Demonstraciniai slaptažodžiai ir `127.0.0.1` prievadai skirti vietiniam naudojimui.

Laikai įvedami Lietuvos laiku: compose.yaml Java procesui nustato Europe/Vilnius. Datą galima pasirinkti kalendoriuje, o valandą (07–22) ir minutę (00 arba 30) – iš 24 valandų formato sąrašų. Rezervacijos pradžia galima kas 30 minučių, kaip rodoma skiltyje „Laisvi laikai“. Skalbimo laikas yra 07:00–23:00; administratorius gali pasirinkti 1–960 minučių trukmę.

Sustabdymas: `docker compose down`. PostgreSQL duomenys išlieka Docker tome; jo trinti nereikia. `setup` paslauga pritaiko [accounts.sql](database/accounts.sql) ir jau anksčiau sukurtai vietinei bazei.

## Ką galima patikrinti

- A gyventojas rezervuoja savo mašiną būsimam laikui, mato rezervaciją ir gali ją atšaukti. Jei mašina tuo metu užimta, rezervacija atmetama.
- A gyventojo `GET /api/reservations?dormitoryId=2` grąžina 403. Be tinkamo prisijungimo API grąžina 401.
- A administratorius keičia A trukmę iš 60 į 90 minučių; B trukmė lieka 60. Rezervacija nuo 15:00 A baigiasi 16:30, B – 16:00. Jei DB jau naudota, prieš bandymą abiejų administratorių formose nustatykite 60.
- Gyventojas negali keisti administratoriaus nustatymų (403). A administratorius negali pakeisti B nustatymų (403).

Puslapis naudoja HTML formas be JavaScript. Prisijungus serveris sukuria 8 valandų sesiją ir naršyklei perduoda `HttpOnly` slapuką; atsijungus sesija panaikinama. Perkrovus serverį reikia prisijungti iš naujo. JSON adresai: `GET /api/reservations` ir `GET /api/availability?machineId=2&date=2030-11-20`. Naršyklė juos pasiekia su sesijos slapuku; tiesioginės API užklausos taip pat priima `Authorization: Basic` antraštę, bet naršyklės puslapiai ją ignoruoja.

## Automatiniai testai

Po Docker Desktop paleidimo projekto aplanke vykdykite:

```bash
docker compose run --build --rm app mvn test
```

2026-11-22 vietoje paleista `docker compose run --build --rm app mvn test`: **30 testų, 0 nesėkmių, 0 klaidų, 0 praleidimų, BUILD SUCCESS**. Integracijos testai vykdyti su PostgreSQL.

| Testų klasė | Ką įrodo |
| --- | --- |
| [ReservationServiceTest](src/test/java/ReservationServiceTest.java) | Pagrindinė rezervavimo taisyklė, intervalų ribos, bendrabučių ir gyventojų atskyrimas |
| [ReservationSettingsTest](src/test/java/ReservationSettingsTest.java) | A 90 / B 60 minučių, laisvi laikai ir prasidėjusių rezervacijų ribojimas |
| [PostgresReservationRepositoryTest](src/test/java/PostgresReservationRepositoryTest.java) | Rezervacijos įrašymas, perskaitymas nauju DB prieigos objektu ir bendrabučių atskyrimas |
| [WebServerIntegrationTest](src/test/java/WebServerIntegrationTest.java) | HTTP prisijungimas, rolės, 403 svetimam bendrabučiui, paskyros kūrimas ir nustatymo poveikis |

Iš viso yra 25 vienetiniai ir 5 integraciniai testai. Pirmosios dvi klasės veikia be DB. Dvi integracijos testų klasės pilnai vykdomos nurodyta Docker komanda; be `JDBC_URL` jų testai praleidžiami.

## Kur rasti kodą

| Veiksmas | Pradėti nuo |
| --- | --- |
| Paleidimas ir URL parinkimas | [WebServer.main / handle](src/main/java/WebServer.java) |
| Prisijungimas ir bendrabučio teisės | [WebServer](src/main/java/WebServer.java), [PostgresAccounts](src/main/java/PostgresAccounts.java) |
| Rezervacijos sukūrimas ir taisyklės | [WebServer.createReservation](src/main/java/WebServer.java) → [ReservationService.createReservation](src/main/java/ReservationService.java#L20) → [PostgresReservationRepository.save](src/main/java/PostgresReservationRepository.java#L39) |
| Laisvi laikai, savos rezervacijos, atšaukimas | [ReservationService.getAvailableStartTimes](src/main/java/ReservationService.java#L118), `getMyReservations`, `cancelReservation` |
| Atskiri bendrabučio nustatymai | [PostgresSettingsAdmin](src/main/java/PostgresSettingsAdmin.java), [PostgresReservationSettingsSource](src/main/java/PostgresReservationSettingsSource.java#L19) |
| DB lentelės ir paskyros | [init.sql](database/init.sql), [accounts.sql](database/accounts.sql) |

Patvirtinto [I etapo plano](I_etapas_projektavimo_dokumentas.md) keturios gyventojo funkcijos ir palaikomumo tikslas išlaikyti. II etapo rezervavimo taisyklių testai vykdomi ir galutinėje sistemoje. III etape pridėta PostgreSQL, prisijungimas, administratoriaus veiksmai ir paprastas HTML puslapis.
