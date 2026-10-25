# Bendrabučių skalbyklų rezervavimo sistema

## Projekto aprašymas

Bendrabučių skalbyklų rezervavimo sistema – Java konsolinė programa, skirta studentų skalbimo mašinų rezervacijoms valdyti.

Sistema leidžia:

- Kurti naujas skalbimo mašinų rezervacijas.
- Peržiūrėti savo rezervacijas.
- Atšaukti savo rezervacijas.
- Patikrinti rezervavimo laiko tinkamumą ir galimus konfliktus.
- Atskirti skirtingų bendrabučių ir studentų rezervacijas.

Studentas gali peržiūrėti ir atšaukti tik savo rezervacijas. Šios teisės tikrinamos `ReservationService` klasėje.

Projektas įgyvendintas kaip II etapo veikiantis prototipas, naudojant objektinio programavimo principus ir atskiriant naudotojo sąsają, verslo logiką bei duomenų saugojimą.

## Naudojamos technologijos

- **Java 21** – projekto kompiliavimo versija.
- **Maven** – projekto kūrimo ir priklausomybių valdymo įrankis.
- **Maven Wrapper (Maven 3.9.9)** – leidžia vykdyti Maven komandas be atskiro Maven diegimo.
- **JUnit 5** – automatiniams testams.
- **IntelliJ IDEA** – naudota programai kurti ir testuoti.

Duomenys saugomi operatyviojoje atmintyje naudojant `ArrayList`. Duomenų bazė šiame etape nenaudojama.

## Reikalavimai programai paleisti

Reikalingas **JDK 21 arba naujesnis suderinamas JDK**.

Atskirai diegti Maven nereikia, nes projekte yra Maven Wrapper. Pirmą kartą paleidžiant projektą gali reikėti interneto ryšio Maven ir jo priklausomybėms atsisiųsti.

## Programos paleidimas

Atidaryti terminalą pagrindiniame projekto aplanke, kuriame yra `pom.xml` ir `mvnw`.

**macOS / Linux:**

```bash
./mvnw compile exec:java -Dexec.mainClass=Main
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd compile exec:java "-Dexec.mainClass=Main"
```

Programa paleidžiama viena komanda. Paleidus pateikiamas konsolės meniu:

```text
1 - Sukurti rezervaciją
2 - Mano rezervacijos
3 - Atšaukti rezervaciją
4 - Pakeisti demonstracinį studentą
0 - Baigti programą
```

Naudotojas įveda norimo veiksmo numerį ir vykdo programos pateikiamus nurodymus.

## Rezervavimo taisyklės

Sistemoje įgyvendintos šios rezervavimo taisyklės:

1. Studentas gali rezervuoti tik savo bendrabučio skalbimo mašiną.
2. Rezervacijos pradžia turi būti ateityje.
3. Vienos rezervacijos trukmė – 1 valanda.
4. Rezervacija turi patekti į darbo laiką nuo 07:00 iki 23:00.
5. Ta pati skalbimo mašina negali būti rezervuojama persidengiančiais laiko intervalais.
6. Tas pats studentas negali vienu metu rezervuoti skirtingų skalbimo mašinų.
7. Gretimos rezervacijos leidžiamos, jeigu jų laikai nesikerta.

Pavyzdžiui, jei skalbimo mašina rezervuota nuo 15:00 iki 16:00:

- Rezervacija nuo 15:30 atmetama, nes laikas persidengia.
- Rezervacija nuo 16:00 leidžiama, nes ankstesnė rezervacija jau pasibaigusi.

Rezervacijos pabaiga gali sutapti su darbo laiko pabaiga – 23:00.

Sistema taip pat atmeta neteisingas kalendorines datas, pavyzdžiui, vasario 30 dieną.

## Demonstraciniai duomenys

Programoje iš anksto sukurti du demonstraciniai studentai:

| Studentas | Studento ID | Bendrabučio ID |
|---|---|---|
| Studentas A | 101 | 1 |
| Studentas B | 101 | 2 |

Kiekviename bendrabutyje yra dvi skalbimo mašinos: Nr. 2 ir Nr. 3.

Studentų ir skalbimo mašinų numeriai gali sutapti skirtinguose bendrabučiuose, nes objektai atskiriami pagal bendrabučio ID. Rezervacijos priklausymas studentui tikrinamas pagal bendrabučio ID ir studento ID.

Meniu pasirinkimas `4` leidžia demonstravimo metu perjungti aktyvų studentą. Tai yra demonstravimo funkcija, o ne tikra autentifikavimo sistema.

## Demonstravimo scenarijus

Paleidus programą galima patikrinti skirtingų bendrabučių duomenų atskyrimą:

1. Studentas A rezervuoja mašiną Nr. 2 laikui `2030-11-22 15:00`.
2. Pasirinkus `4`, programa perjungiama į Studentą B.
3. Studentas B pasirenka rezervacijų peržiūrą. Sistema parodo, kad rezervacijų nėra.
4. Studentas B rezervuoja savo bendrabučio mašiną Nr. 2 tam pačiam laikui.
5. Programa perjungiama į Studentą A.
6. Studentas A bando atšaukti Studento B rezervaciją pagal jos numerį. Sistema atmeta šį veiksmą.
7. Perjungus į Studentą B patikrinama, kad jo rezervacija liko išsaugota.

Šis scenarijus parodo, kad rezervacijos atskiriamos pagal bendrabutį ir kito bendrabučio rezervacijų negalima peržiūrėti ar atšaukti.

## Automatiniai testai

Projekte naudojami JUnit 5 automatiniai testai. Šiuo metu įgyvendinta **20 automatinių testų**:

- 19 testų `ReservationServiceTest.java`.
- 1 testas `WashingMachineTest.java`.

Testai tikrina:

- Sėkmingą rezervacijos sukūrimą.
- Netinkamo rezervavimo laiko atmetimą.
- Darbo laiko ribas.
- Tos pačios skalbimo mašinos rezervacijų konfliktus.
- To paties studento rezervacijų konfliktus.
- Gretimų rezervacijų leidimą.
- Skirtingų bendrabučių duomenų atskyrimą.
- Skirtingų studentų rezervacijų privatumą tame pačiame bendrabutyje.
- Draudimą atšaukti svetimas rezervacijas.
- Savo rezervacijos atšaukimą.

Testuose naudojamas fiksuotas laikas (`Clock`), todėl jų rezultatai nepriklauso nuo dabartinės datos.

### Testų paleidimas

**macOS / Linux:**

```bash
./mvnw test
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd test
```

Sėkmingai įvykdžius testus, Maven pateikia rezultatą `BUILD SUCCESS`.

## Projekto struktūra

| Failas | Paskirtis |
|---|---|
| `Main.java` | Konsolės meniu, naudotojo įvestis ir rezultatų pateikimas |
| `ReservationService.java` | Rezervavimo verslo logika ir prieigos teisių tikrinimas |
| `ReservationRepository.java` | Rezervacijų saugojimas atmintyje, paieška ir šalinimas |
| `Reservation.java` | Rezervacijos duomenų modelis |
| `User.java` | Studento duomenų modelis |
| `WashingMachine.java` | Skalbimo mašinos duomenų modelis |
| `Dormitory.java` | Bendrabučio duomenų modelis |
| `ReservationServiceTest.java` | Rezervavimo logikos JUnit testai |
| `WashingMachineTest.java` | Skalbimo mašinos modelio testas |
| `pom.xml` | Maven projekto nustatymai |
| `mvnw`, `mvnw.cmd` | Maven Wrapper paleidimo failai |

### Architektūros principai

Projekte atskirtos pagrindinės atsakomybės:

- `Main` atsakinga už naudotojo sąsają.
- `ReservationService` atsakinga už rezervavimo taisykles.
- `ReservationRepository` atsakinga už duomenų saugojimą.
- Modelių klasės saugo atitinkamų objektų duomenis.

`ReservationService` gauna `ReservationRepository` per konstruktorių, taip naudojamas priklausomybių perdavimas (*dependency injection*).

Architektūroje siekiama laikytis objektinio programavimo, vienos atsakomybės principo (SRP) ir švaraus kodo praktikų, vengiant nereikalingo sudėtingumo.

## II etapo apimtis ir apribojimai

Šiame etape įgyvendintas veikiantis konsolinis rezervavimo prototipas.

Dabartiniai apribojimai:

- Duomenys saugomi tik programos veikimo metu. Išjungus programą rezervacijos neišlieka.
- Duomenų bazė nenaudojama.
- Tikras naudotojų prisijungimas ir autentifikavimas neįgyvendinti.
- Rezervacijų trukmė visiems bendrabučiams yra vienoda – 1 valanda.
- Darbo laikas visiems bendrabučiams vienodas – 07:00–23:00.
- Atskiros laisvų rezervavimo laikų peržiūros funkcijos nėra.
- Rezervacijų peržiūra nefiltruoja jau praėjusių rezervacijų.
- Atšaukimo funkcija atskirai netikrina, ar rezervacija dar neprasidėjusi.

I etapo projektavimo dokumente numatyti platesni funkcionalumo sprendimai gali būti plėtojami kituose projekto etapuose.

## Projekto paskirtis

Projektas parengtas VILNIUS TECH programų inžinerijos studijų dalyko „Programų sistemų projektavimas“ II etapo praktiniam atsiskaitymui.
