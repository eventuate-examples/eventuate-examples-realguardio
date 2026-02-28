PNGS := $(patsubst %.txt,%.png,$(wildcard **/*.txt))

all: $(PNGS)

%.png : %.txt
	run-plantuml.sh $< $@
