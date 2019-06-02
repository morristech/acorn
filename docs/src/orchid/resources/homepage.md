---
extraCss:
    - |
        inline:.scss:
        .image-preview {
            text-align: center; 
            img {
                max-width:80%;
            }    
        }
---

Acorn turns your navigation back stack in a composable and decoupled navigation
structure.

At its core, Acorn is a set of interfaces that describe the basics of mobile
screen navigation: a {{ anchor('Scene') }} representing a screen and regarded
as a basic building block for the application flow, and a
{{ anchor('Navigator') }} which controls this application flow.
{{ anchor('Containers','Container') }} form the boundary between your
presentation layer and the UI elements.

![]({{ 'media/acorn_diagram.svg'|asset }})
{.image-preview}

On top of this, Acorn provides an extensive set of default implementations to
do the work for you: several base Scene implementations that provide the basics,
and a couple of Navigators that can be composed together to create the
navigational structure that you need.

Acorn decouples the UI from navigation, meaning the user interface becomes a
plugin into the navigational state.
This gives you full control over your view elements and especially your
transition animations.

In the {{ anchor('Wiki') }} section you can find information on several topics 
when working with Acorn.

_Note: this documentation website is work in progress, and some sections may be
missing or incomplete._
