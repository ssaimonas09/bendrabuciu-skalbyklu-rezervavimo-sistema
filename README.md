# Bendrabučių skalbyklų rezervavimo sistema

## Projekto aprašymas

**Bendrabučių skalbyklų rezervavimo sistema** – Java konsolinė programa, skirta studentų skalbimo mašinų rezervacijoms valdyti. Tai „Programų sistemų projektavimo“ dalyko **II etapo veikiantis prototipas**, kuriame pagrindinis dėmesys skiriamas rezervacijos sukūrimui, verslo taisyklių tikrinimui ir skirtingų bendrabučių duomenų atskyrimui.

Programa leidžia:

- sukurti skalbimo mašinos rezervaciją;
- peržiūrėti savo rezervacijas;
- atšaukti savo rezervaciją;
- patikrinti, ar rezervacijos laikas tinkamas ir nesikerta su esamomis rezervacijomis;
- demonstravimo metu perjungti du skirtingiems bendrabučiams priklausančius studentus.

Rezervacijų peržiūros ir atšaukimo teisės tikrinamos `ReservationService` klasėje. Tikro prisijungimo ar autentifikavimo šiame prototipe nėra – naudojami iš anksto sukurti demonstraciniai studentai.

## Naudojamos technologijos

| Technologija | Paskirtis |
|---|---|
| Java 21 | Projekto kompiliavimo tikslinė versija |
| Maven | Projekto kompiliavimas ir priklausomybių valdymas |
| Maven Wrapper (Maven 3.9.9) | Maven komandų vykdymas be atskiro Maven diegimo |
| JUnit 5 (`junit-jupiter` 5.14.3) | Automatiniai vienetų testai |
| Maven Surefire Plugin 3.5.6 | JUnit testų vykdymas per Maven |
| IntelliJ IDEA | Programos kūrimo ir testavimo aplinka |

Rezervacijos saugomos operatyviojoje atmintyje naudojant `ArrayList`. **Duomenų bazė šiame etape nenaudojama.**

## Reikalavimai paleidimui

Reikalingas **JDK 21** arba naujesnis su Java 21 suderinamas JDK.

Projektas turi Maven Wrapper, todėl Maven atskirai diegti nereikia. Pirmą kartą vykdant komandą gali reikėti interneto ryšio Maven ir priklausomybėms atsisiųsti.

Komandas reikia vykdyti **pagrindiniame projekto aplanke**, kuriame yra `pom.xml`, `mvnw` ir `mvnw.cmd`.

## Programos paleidimas

**macOS / Linux:**

```bash
./mvnw compile exec:java -Dexec.mainClass=Main
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd compile exec:java "-Dexec.mainClass=Main"
```

Paleidus programą rodomas aktyvus studentas, jo bendrabučio numeris ir meniu:

```text
1 - Sukurti rezervaciją
2 - Mano rezervacijos
3 - Atšaukti rezervaciją
4 - Pakeisti demonstracinį studentą
0 - Baigti programą
```

Pasirinkus `1`, reikia įvesti skalbimo mašinos numerį (`2` arba `3`) ir pradžios datą bei laiką, pavyzdžiui, `2030-11-22 15:00`. Rezervacijos pabaigą sistema apskaičiuoja automatiškai.

## Rezervavimo taisyklės

1. Studentas gali rezervuoti **tik savo bendrabučio** skalbimo mašiną.
2. Rezervacijos pradžia turi būti **ateityje**; rezervavimas tiksliai dabartiniu momentu neleidžiamas.
3. Viena rezervacija trunka **1 valandą**.
4. Visas rezervacijos intervalas turi tilpti į **07:00–23:00** darbo laiką. Galimos, pavyzdžiui, 07:00–08:00 ir 22:00–23:00 rezervacijos.
5. Negalima sukurti dviejų **tos pačios mašinos** persidengiančių rezervacijų tame pačiame bendrabutyje.
6. Tas pats studentas negali tuo pačiu metu rezervuoti **dviejų skirtingų mašinų**.
7. **Gretimos rezervacijos leidžiamos:** 15:00–16:00 ir 16:00–17:00 nesikerta.
8. Studentas gali **peržiūrėti ir atšaukti tik savo** rezervacijas, ne kito studento ar kito bendrabučio.

Pavyzdys: jei mašina rezervuota **15:00–16:00**, naujas bandymas rezervuoti ją **15:30–16:30** atmetamas, o **16:00–17:00** – leidžiamas.

Datos įvedimui naudojamas formatas `uuuu-MM-dd HH:mm` ir `ResolverStyle.STRICT`, todėl neteisingos kalendorinės datos (pavyzdžiui, `2030-02-30 15:00`) atmetamos. Ši datos įvedimo taisyklė veikia `Main.java`; jai atskiro JUnit testo šiuo metu nėra.

## Demonstraciniai duomenys

Programoje iš anksto sukurti du studentai:

| Studentas | Studento ID | Bendrabučio ID |
|---|---:|---:|
| Studentas A | 101 | 1 |
| Studentas B | 101 | 2 |

Abiejuose bendrabučiuose yra po dvi skalbimo mašinas:

| Bendrabutis | Mašinos numeriai |
|---|---|
| 1 | 2 ir 3 |
| 2 | 2 ir 3 |

**Studentų ir mašinų ID specialiai kartojasi skirtinguose bendrabučiuose.** Tai padeda parodyti, kad duomenys turi būti atskiriami pagal bendrabučio ID, o ne vien pagal studento ar mašinos numerį.

Meniu pasirinkimas `4` pakeičia aktyvų demonstracinį studentą. Tai **nėra autentifikavimo sistema**.

## Programos demonstravimo scenarijai

Toliau pateikiami veiksmai, kuriuos galima parodyti paleidus programą. Pasirinkta data yra demonstracinė; jei ji jau būtų praėjusi, reikia pasirinkti vėlesnę.

### 1. Sėkmingas rezervavimas ir laiko konfliktas

1. Pasirinkti Studentą A (jis aktyvus paleidžiant programą).
2. Pasirinkti `1`, mašiną `2`, laiką `2030-11-22 15:00` – rezervacija sukuriama.
3. Dar kartą pasirinkti `1`, mašiną `2`, laiką `2030-11-22 15:30` – rezervacija atmetama, nes laikas persidengia.
4. Pasirinkti `1`, mašiną `2`, laiką `2030-11-22 16:00` – gretima rezervacija leidžiama.
5. Pasirinkti `2` – matomos Studento A rezervacijos.

### 2. Dviejų bendrabučių duomenų atskyrimas

1. Studentas A rezervuoja savo bendrabučio mašiną `2` laikui `2030-11-23 15:00`.
2. Pasirinkti `4` ir pereiti prie Studento B.
3. Pasirinkti `2` – Studento A rezervacija **nerodoma**.
4. Studentas B gali savo bendrabučio mašiną `2` rezervuoti **tuo pačiu laiku**, nes tai kita mašina kitame bendrabutyje.
5. Užsirašyti naujos Studento B rezervacijos numerį.
6. Pasirinkti `4` ir grįžti prie Studento A.
7. Pasirinkti `3` ir įvesti Studento B rezervacijos numerį – atšaukimas **atmetamas**.
8. Grįžti prie Studento B ir pasirinkti `2` – jo rezervacija tebėra išsaugota.

Skirtingų studentų privatumas **tame pačiame bendrabutyje** taip pat tikrinamas atskirais JUnit testais. Konsoliniame meniu demonstravimui iš anksto sukurti tik du studentai iš skirtingų bendrabučių.

## Automatiniai testai (JUnit 5)

Iš viso projekte yra **20 automatinių testų**:

- **19** testų faile `src/test/java/ReservationServiceTest.java`;
- **1** testas faile `src/test/java/WashingMachineTest.java`.

### Rezervacijų kūrimas ir konfliktai – 7 testai

| Nr. | Testo metodas | Ką tikrina |
|---:|---|---|
| 1 | `createsReservationInOwnDormitory()` | Studentas gali sukurti rezervaciją savo bendrabutyje; saugykloje atsiranda vienas įrašas. |
| 2 | `rejectsMachineFromOtherDormitory()` | Bandymas rezervuoti svetimo bendrabučio mašiną atmetamas; rezervacija neišsaugoma. |
| 3 | `rejectsOverlappingReservation()` | Antras studentas negali rezervuoti tos pačios mašinos 15:30, kai ji užimta 15:00–16:00. |
| 4 | `allowsAdjacentReservations()` | Skirtingi studentai gali rezervuoti tą pačią mašiną 15:00–16:00 ir 16:00–17:00. |
| 5 | `rejectsOverlappingReservationsForSameUser()` | Tas pats studentas negali rezervuoti kitos mašinos persidengiančiu laiku. |
| 6 | `allowsAdjacentReservationsForSameUser()` | Tas pats studentas gali rezervuoti skirtingas mašinas gretimais intervalais. |
| 7 | `rejectsReservationOverlappingFromEarlierStart()` | Atmetama 14:30–15:30 rezervacija, kai ta pati mašina jau užimta 15:00–16:00. |

### Laiko tikrinimas – 6 testai

| Nr. | Testo metodas | Ką tikrina |
|---:|---|---|
| 8 | `rejectsReservationBeforeOpeningTime()` | Atmetama rezervacija nuo 06:30 – prieš darbo pradžią. |
| 9 | `rejectsReservationAfterClosingTime()` | Atmetama rezervacija nuo 22:30, nes jos pabaiga būtų 23:30. |
| 10 | `allowsReservationEndingAtClosingTime()` | Leidžiama rezervacija nuo 22:00 iki 23:00. |
| 11 | `rejectsPastReservation()` | Neleidžiama kurti rezervacijos praeityje. |
| 12 | `rejectsReservationAtCurrentTime()` | Neleidžiama kurti rezervacijos, prasidedančios tiksliai dabartiniu momentu. |
| 13 | `allowsReservationStartingAtSeven()` | Leidžiama rezervacija tiksliai 07:00–08:00; tikrinamas jos pradžios ir pabaigos laikas. |

### Studentų ir bendrabučių duomenų atskyrimas – 4 testai

| Nr. | Testo metodas | Ką tikrina |
|---:|---|---|
| 14 | `studentACannotSeeDormitoryBReservations()` | Studentas A mato tik savo bendrabučio rezervaciją, nors kito bendrabučio studento ID taip pat yra `101`. |
| 15 | `cannotCancelOtherDormitoryReservation()` | Studentas A negali atšaukti Studento B rezervacijos iš kito bendrabučio; rezervacija lieka išsaugota. |
| 16 | `cannotSeeOtherStudentsReservationsInSameDormitory()` | Studentas mato tik savo, o ne kito studento rezervacijas tame pačiame bendrabutyje. |
| 17 | `cannotCancelOtherStudentsReservationInSameDormitory()` | Studentas negali atšaukti kito to paties bendrabučio studento rezervacijos. |

### Rezervacijos atšaukimas – 2 testai

| Nr. | Testo metodas | Ką tikrina |
|---:|---|---|
| 18 | `ownerCanCancelOwnReservation()` | Studentas gali atšaukti savo rezervaciją; po atšaukimo jos nebemato savo sąraše. |
| 19 | `allowsReservationAfterCancellation()` | Atšaukus rezervaciją galima iš naujo rezervuoti tą pačią mašiną tuo pačiu laiku. |

### Skalbimo mašinos duomenys – 1 testas

| Nr. | Testo metodas | Ką tikrina |
|---:|---|---|
| 20 | `washingMachineHasCorrectDormitory()` | `WashingMachine` objektas teisingai grąžina jam priskirtą bendrabučio ID. |

### Kaip veikia testai?

`ReservationServiceTest.java` naudojamas **fiksuotas laikrodis** (`Clock.fixed()`), nustatytas į `2030-11-19 12:00 UTC`. Tai leidžia patikrinti „praeities“, „dabarties“ ir „ateities“ scenarijus nepriklausomai nuo realios testų paleidimo datos.

Svarbiausi JUnit patikrinimai:

- `assertEquals(expected, actual)` – patikrina, ar faktinis rezultatas lygus laukiamam;
- `assertThrows(...)` – patikrina, ar neteisingas veiksmas sukelia numatyto tipo išimtį.

### Testų paleidimas

**macOS / Linux:**

```bash
./mvnw clean test
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd clean test
```

Norint paleisti testus be išankstinio išvalymo, galima naudoti `./mvnw test` (Windows – `.\mvnw.cmd test`).

Sėkmingai įvykdžius testus, Maven pateikia `BUILD SUCCESS`. Naujausią rezultatą reikia patikrinti paleidžiant testus vietiniame projekto aplanke.

## Projekto struktūra

```text
.
├── pom.xml
├── README.md
├── I_etapas_projektavimo_dokumentas.md
├── mvnw
├── mvnw.cmd
├── .mvn/
└── src/
    ├── main/java/
    │   ├── Main.java
    │   ├── ReservationService.java
    │   ├── ReservationRepository.java
    │   ├── Reservation.java
    │   ├── User.java
    │   ├── WashingMachine.java
    │   └── Dormitory.java
    └── test/java/
        ├── ReservationServiceTest.java
        └── WashingMachineTest.java
```

| Failas / klasė | Pagrindinė atsakomybė |
|---|---|
| `Main` | Konsolės meniu, įvestis, demonstracinių studentų parinkimas, rezultatų pateikimas |
| `ReservationService` | Rezervavimo taisyklių, konfliktų ir prieigos teisių tikrinimas |
| `ReservationRepository` | Rezervacijų saugojimas, paieška, ID generavimas ir šalinimas |
| `Reservation` | Rezervacijos ID, studento, bendrabučio, mašinos ir laiko duomenys |
| `User` | Studento ID, vardas ir bendrabučio ID |
| `WashingMachine` | Skalbimo mašinos ID ir jos bendrabučio ID |
| `Dormitory` | Bendrabučio ID ir pavadinimo modelis; dabartinėje rezervavimo logikoje nenaudojamas |
| `ReservationServiceTest` | Rezervavimo logikos ir duomenų izoliavimo testai |
| `WashingMachineTest` | Skalbimo mašinos modelio testas |
| `pom.xml` | Maven ir JUnit projekto nustatymai |

## OOP ir architektūros principai

Programa suskirstyta į **konsolinę sąsają**, **verslo logiką** ir **duomenų saugojimą**. Taip atskiriamos klasių atsakomybės ir kodą lengviau suprasti bei testuoti.

- **Inkapsuliacija:** duomenų modelių laukai yra `private final`; jų reikšmės pasiekiamos per `get...()` metodus.
- **Vienos atsakomybės principas (SRP):** `Main` apdoroja įvestį, `ReservationService` tikrina taisykles, o `ReservationRepository` saugo rezervacijas.
- **Priklausomybių perdavimas per konstruktorių:** `ReservationService` gauna `ReservationRepository` objektą per konstruktorių.
- **Testuojamumas:** `ReservationService` konstruktorius taip pat gali priimti `Clock`, todėl testuose naudojamas fiksuotas laikas.

Projektas sąmoningai paliktas paprastas: nenaudojami papildomi karkasai, sudėtingi projektavimo šablonai ar nereikalingos abstrakcijos. Priklausomybių perdavimas per konstruktorių **nereiškia**, kad čia visiškai įgyvendintas kiekvienas SOLID principas.

## II etapo apimtis ir apribojimai

Įgyvendintas veikiantis **konsolinis rezervavimo prototipas**. Svarbūs dabartinės versijos apribojimai:

- Duomenys išsaugomi tik programos veikimo metu; uždarius programą jie prarandami.
- Duomenų bazė neįdiegta.
- Tikro prisijungimo ir autentifikavimo nėra; aktyvus studentas keičiamas demonstraciniu meniu.
- Visiems bendrabučiams taikoma ta pati **1 valandos** rezervacijos trukmė ir **07:00–23:00** darbo laikas.
- Atskiros laisvų rezervavimo laikų peržiūros funkcijos nėra.
- „Mano rezervacijos“ sąraše jau praėjusios rezervacijos nefiltruojamos.
- Atšaukiant rezervaciją tikrinama jos priklausomybė studentui, bet **netikrinama**, ar rezervacija jau prasidėjusi.
- `Dormitory` modelis yra projekte, tačiau kol kas nėra naudojamas pagrindinėje rezervavimo logikoje; bendrabučiui atskirti naudojamas `dormitoryId`.

I etapo projektavimo dokumente numatytas platesnis funkcionalumas gali būti plėtojamas vėlesniuose projekto etapuose.

## Projekto paskirtis

Projektas parengtas VILNIUS TECH programų inžinerijos studijų dalyko **„Programų sistemų projektavimas“ II etapo** praktiniam atsiskaitymui.
