.PHONY: pdf clean

pdf:
	asciidoctor-pdf -r asciidoctor-diagram -a imagesoutdir=images architecture.adoc

clean:
	rm -f architecture.pdf
	rm -rf images/

install-diagram-tools:
	npm install -g @mermaid-js/mermaid-cli
	gem install asciidoctor-diagram-plantuml