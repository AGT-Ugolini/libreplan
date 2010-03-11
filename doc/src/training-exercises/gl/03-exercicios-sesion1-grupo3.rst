Exercicios Grupo 3
##################

.. contents::


Exercicio  1. Criterios
=======================

Crear cada grupo o tipo de criterio cos criterios asociados  detallados:



      * Nome do tipo de  criterio: Capacitación de soldadura.
      * Permite valores  simultáneos: Non.
      * Xerárquico: Non.
      * Criterios:

         * Soldadura  primeiro nivel.
         * Soldadura segundo nivel.
         * Soldadura terceiro nivel.

Exercicio 2. Calendarios
========================

Crear cada grupo un calendario global na aplicación cos seguintes datos:



      * Calendario Andalucía xornada completa.
      * Deriva de España.
      * Días excepcionais:

         * 3 de Maio - Tipo BankHoliday - Horas  laborais 0.
         * 24 de Decembro - Tipo Workable  BankHoliday - Horas laborais 4.
         * 31 de Decembro  - Tipo Workable BanHoliday - Horas laborais 4.

      * Xornada semanal:

         * Luns - 9 h.
         * Martes - 9 h.
         * Mércores - 9 h.
         * Xoves - 9 h.
         * Venres - 4 h.

Exercicio 3. Etiquetas
======================

Crear os  tipos de etiqueta e etiquetas seguintes:



      * Tipo de  etiqueta: Nacionalidade de cliente.
      * Valores:

         * España.
         * Estados Unidos.
         * Portugal.
         * Reino Unido.
         * Brasil.

Exercicio 4. Recursos
=====================

Crear os seguintes recursos cos datos seguintes:



      * Nome: Alfonso Domínguez Baúl. NIF:  5555555E Calendario derivado: Asturias.
      * Nome: Alicia Muñoz Lages.  NIF: 66666666D Calendario derivado: Galicia.
      * Nome: Elias Baeza Robles. NIF: 77777777H Calendario derivado: Asturias.

Exercicio 5. Períodos Actividade
================================

Configurar os seguintes períodos de  actividade para os traballadores.



      * Nome: Alfonso  Domínguez Baúl.

         * Data contratación: 08/03/2010 -  Indefinido.

      * Nome: Alicia Muñoz Lages.

         * Data contratación:  15/03/2010 - 15/09/2011.

      * Nome: Elias Baeza Robles.

         * Data contratación: 01/03/2010 - Indefinido.

Exercicio 6 - Configurar Excepcións de Calendario
=================================================

Configurar as vacacións como exception  days de intervalo e de tipo HOLIDAY.



      * Nome: Alfonso Domínguez Baúl.

         * Vacacións: 21/06/2010 - 27/06/2010

      * Nome: Alicia Muñoz Lages.

         * Vacacións: 28/06/2010 - 04/07/2010

Exercicio 7 -  Criterios en recursos
====================================

Configurar a satisfacción de criterios por parte dos recursos.



      * Nome: Alfonso Domínguez Baúl.
      * Satisfaccións  de criterio:

         * Grupo: Grupo 3  -  Dende 01/04/2010 ata infinito.
         * Tipo de   traballo: Carpinteiro  - Dende 01/04/2010 ata infinito.

      * Nome: Alicia Muñoz Lages.
      * Satisfaccións   de criterio:

         * Grupo: Grupo 3   -  Dende 01/04/2010 ata infinito.
         * Tipo de traballo: Pintor   - Dende 01/04/2010 ata infinito.

      * Nome: Elías Baeza Robles.
      * Satisfaccións de criterio:

         * Grupo: Grupo 3 - Dende 01/03/2010 ata infinito.

Exercicio  8. Creación dun pedido
=================================

Crear un pedido cada grupo cos seguintes datos e poñerlle, os puntos de planificación e os criterios indicados:



      * Datos de pedido:

         * Nome:  Pedido Grupo 3.
         * Data inicio: 08/03/2010
         * Data  limite:  01/06/2011.
         * Cliente: Barreras.
         * Responsable: Nome da persoa  do grupo.
         * Presuposto: Traballo:  100.000  Materiais: 0
         * Calendario:  Galicia.
         * Estado:  Ofertado.

      * Elementos de pedido:

         * 1.   Coordinacion - Grupo 3

            * 1.1 Reunións con cliente - 100h *Punto de  planificación*
            * 1.2  Reunións con traballadores - 100h *Punto  de planificación*

         * 2  Bloque 1   *Punto de planificación* - Grupo 3

            * 2.1 Pintar camarotes A e B- 350 h - Pintor
            * 2.2 Pintar sala de  máquinas - 200 h - Pintor
            * 2.3 Pintas cociña de buque - 100 h - Pintor

         * 3 Bloque  2 - Grupo 3

            * 3.1 Teito de  madeira de camarote A - 300 h *Punto de planificación* - Carpinteiro
            * 3.2  Cama e  mesilla de camarote A - 250 h *Punto de planificación* - Carpinteiro
            * 3.3  Poñer  escotillas  camarote A - 200 h *Punto de planificación* - Carpinteiro

Exercicio  9 - Planificando dependencias
========================================

Poñer as dependencias seguintes na planificación de cada pedido:



Poñer  as seguintes dependencias:

         * Bloque 1 FIN-INICIO Bloque 2
         * Pintar camarotes A e B FIN-INICIO Pintar  sá de máquinas
         * Pintar  sá de máquinas FIN-INICIO Pintar cociñas de buque.
         * Teito de madeira de camarote A INICIO-INICIO Cama e mesillas de camarote A
         * Teito de madeira de camarote A FIN-INICIO Poñer escotillas camarote A
         * Crear un fito  chamado Recepción de material que sexa o 14/10/2010
         * Fito recepción de material FIN-INICIO Poñer escotillas camarote A

Exercicio 10. Asignación de recursos
====================================

Realizar as seguintes asignacións



      *  Tarefa:  Coordinación:

         * Asignación  específica: Elias Baeza Robles
         * Estratexia: - Calcula data fin
         * Número  de  recursos por dia: 0.6

      * Tarefa: Pintar camarotes A e B

         *  Asignación xenérica
         * Estratexia  recomendada
         * Número  de recursos por dia: 1

      *  Tarefa: Pintar sá de máquinas

         * Asignación xenérica
         * Estratexia  recomendada
         * Número  de recursos por dia: 1

      *  Tarefa: Pintar cociñas de buque

         * Asignación xenérica
         * Estratexia  recomendada
         * Número  de recursos por dia: 1

      *  Tarefa: Teito de madeira de camarote A

         * Asignación xenérica con criterios [Grupo 3, Carpinteiro]
         * Estratexia: Calcular recursos por dia.
         * Data de fin: 15 Outubro 2010
         * Horas:  300  horas.

      * Tarefa: Cama e mesillas de camarote A

         * Asignación xenérica con criterios [Grupo  3, Carpinteiro]
         * Estratexia: Calcular número de horas
         * Número de  recursos por dia: 0.5
         * Data de fin: 1 de Setembro 2010

      * Tarefa:  Poñer escotillas camarote A

         * Asignación  xenérica con criterios [Grupo  3, Carpinteiro]
         * Estratexia:  Calcular data fin
         * Recursos por dia: 0.5
         * Horas: 200

Exercicio 11. Avances
======================

Realizar as seguintes asignacións de avance



      *  Elemento de pedido  - Coordinación - Avance de tipo porcentaxe - Valor   máximo 100 -  Propaga

         * Valores: 25% a 15 Marzo de 2010.

      *  Elemento  de pedido - Pintar camarotes A e B - Avance de tipo unidades -  Valor  máximo 5 - Propaga

         * Valores: 1  unidade ao 2 de Marzo de 2010
         *  Valores: 2  unidades ao 30 de Marzo de 2010

      * Elemento de pedido  -  Pintar sa de maquinas - Avance de tipo unidades - Valor máximo 10 -   Propaga

         * Valores:  3 unidades ao 2 de Abril de   2010.

      * Elemento de pedido - Pintar cociñas buque -  Avance de tipo unidades - Valor máximo 15 - Propaga

          *  Valores: 5 unidades a 31 de Marzo de 2010.

      *  Elemento de pedido -  Bloque 2 - Avance de tipo porcentaxe - Valor  máximo 100 - Propaga

         *  Valores: 5 a 16  de Marzo de 2010.

      *  Configurar a nivel de pedido  que o  avance  de tipo children é o que  propaga.

