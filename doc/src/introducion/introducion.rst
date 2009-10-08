Introduci�n
##############

.. contents::

A aplicaci�n para xesti�n da produci�n do sector auxiliar do naval pretende resolver principalmente o problema da planificaci�n nas empresas pertencentes � sector. Para elo desenvolv�ronse unha serie de funcionalidades que dan soluci�n a certos problemas detectados durante a an�lise do proxecto.

A modo de resumo, poder�amos destacar os conceptos b�sicos cos que traballar� a aplicaci�n
   * Criterios: Os criterios son unha entidade do sistema que permitir�n clasificar os recursos (tanto humanos como m�quinas) e as tarefas. Os recursos satisfar�n criterios e por outro lado as tarefas requiren criterios para ser realizadas.
   * Calendarios: Os calendarios determinar�n as horas produtivas dispo�ibles dos diferentes recursos. O usuario poder� crear calendarios xerais da empresa e derivar as caracter�sticas para calendarios m�is concretos, chegando ata a nivel de calendario por recurso ou tarefa.
   * Avances: A aplicaci�n permitir� xestionar diversos tipos de avances. Un proxecto pode ser medido en porcentaxe de avance, sen embargo, pode querer ser medido en unidades, presuposto acordado, etc. Ser� responsabilidade da persoa que xestiona a planificaci�n decidir qu� tipo de avance ser� utilizado para contrastar avances a niveis superiores de proxecto. 
   * Recursos: Ser�n de dous tipos diferentes: humanos e m�quinas. Os recursos humanos ser�n os traballadores da empresa que se utilizar�n para controlar a carga da empresa e de uso dos mesmos. Por outro lado, as m�quinas, dependentes das persoas que as xestionan, ser�n outros recursos que tam�n ser�n controlables na aplicaci�n.
   * Pedido e elementos de pedido: Os traballos solicitados polos clientes ter�n un reflexo na aplicaci�n en forma de pedido, que se estrutura en elementos de pedido. O pedido cos seus elementos conformar�n unha estrutura xer�rquina en n niveis. Esta �rbore de elementos ser� sobre a que se traballe � hora de planificar traballos.
   * Tarefas: As tarefas son los elementos de planificaci�n da aplicaci�n. Ser�n utilizadas para temporalizar os traballos a realizar. As caracter�sticas m�is importantes das tarefas ser�n: te�en dependencias entre si e poden requerir criterios a ser satisfeitos para asignar recursos.
   * Partes de traballo: Son os partes dos traballadores das empresas, indicando as horas traballadas e por outro lado as tarefas asignadas �s horas que un traballador realizou. Con esta informaci�n, o sistema � capaz de calcular cantas horas foron consumidas dunha tarefa con respecto � total de horas presupostadas, permitindo contrastar os avances respecto do consumo de horas real.
   * Etiquetas: Ser�n elementos que se usar�n para o etiquetado das tarefas dos proxectos. Con estas etiquetas o usuario da aplicaci�n poder� realizar agrupaci�ns conceptuais das tarefas para posteriormente poder consultar informaci�n das mesmas de xeito agrupado e filtrado.


Formularios
===========
Antes de realizar unha exposici�n das distintas funcionalidades asociadas �s m�dulos m�is importantes, � necesario facer unha explicaci�n xeral da filosof�a de navegaci�n e formularios.

Existen 3 tipos de formularios:
   * Formularios con bot�n de *Back*. Estes formularios forman parte de unha navegaci�n m�is completa, e os cambios que se van realizando vanse almacenando en memoria. Os cambios s� se aplican cando o usuario almacena expl�citamente toda pantalla dende a que chegou a dito formulario.
   * Formularios con bot�n de *Save* e *Close*. Estes formularios permiten realizar 2 operaci�ns. A primeira delas almacena e pecha a ventana actual e a segunda delas pecha sen almacenar os cambios.
   * Formularios con bot�n de *Save*, "Save&Close" e "Close. Permiten realizar 3 operaci�ns. A primeira delas almacena pero contin�a no formulario actual. A segunda almacena e pecha o formulario. E a terceira pecha a ventana sen almacenar os cambios.