/*
 * Copyright 2013, Emanuel Rabina (http://www.ultraq.net.nz/)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.net.ultraq.thymeleaf.decorators.html

import nz.net.ultraq.thymeleaf.decorators.SortingStrategy
import nz.net.ultraq.thymeleaf.decorators.xml.XmlElementDecorator
import static nz.net.ultraq.thymeleaf.LayoutDialect.DIALECT_PREFIX_LAYOUT
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE_CONTENT
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE_DECORATOR
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.PROCESSOR_NAME_TITLEPATTERN

import org.thymeleaf.dom.Element

import groovy.transform.TupleConstructor

/**
 * A decorator specific to processing an HTML {@code <head>} element.
 * 
 * @author Emanuel Rabina
 */
@TupleConstructor
class HtmlHeadDecorator extends XmlElementDecorator {

	final SortingStrategy sortingStrategy

	/**
	 * Decorate the {@code <head>} part, appending all of the content
	 * {@code <head>} elements on to the decorator {@code <head>} elements.
	 * 
	 * @param decoratorHtml Decorator's {@code <head>} element.
	 * @param contentHead	Content's {@code <head>} element.
	 */
	@Override
	void decorate(Element decoratorHtml, Element contentHead) {

		// If the decorator has no <head>, then we can just use the content <head>
		def decoratorHead = decoratorHtml.findElement('head')
		if (!decoratorHead) {
			if (contentHead) {
				decoratorHtml.insertChildWithWhitespace(contentHead, 0)
				def contentTitle = contentHead.findElement('title')
				if (contentTitle) {
					contentTitle.removeAttribute(DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN)
				}
			}
			return
		}

		// Copy the content and decorator <title>s
		// TODO: Surely the code below can be simplified?  The 2 conditional
		//       blocks are doing almost the same thing.
		def titleContainer = new Element('title-container')
		def titlePattern = null
		def titleExtraction = { headElement, titleType ->
			def existingContainer = headElement?.findElement('title-container')
			if (existingContainer) {
				def titleElement = existingContainer.children.last()
				titlePattern = titleElement.getAttributeValue(DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN) ?: titlePattern
				titleElement.setNodeProperty(TITLE_TYPE, titleType)
				headElement.removeChildWithWhitespace(existingContainer)
				titleContainer.addChild(existingContainer)
			}
			else {
				def titleElement = headElement?.findElement('title')
				if (titleElement) {
					titlePattern = titleElement.getAttributeValue(DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN) ?: titlePattern
					titleElement.setNodeProperty(TITLE_TYPE, titleType)
					titleElement.removeAttribute(DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN)
					headElement.removeChildWithWhitespace(titleElement)
					titleContainer.addChild(titleElement)
				}
			}
		}
		titleExtraction(decoratorHead, TITLE_TYPE_DECORATOR)
		titleExtraction(contentHead, TITLE_TYPE_CONTENT)

		def resultTitle = new Element('title')
		resultTitle.setAttribute("${DIALECT_PREFIX_LAYOUT}:${PROCESSOR_NAME_TITLEPATTERN}", titlePattern)
		titleContainer.addChild(resultTitle)

		// Merge the content's <head> elements with the decorator's <head>
		// section via the given merging strategy, placing the resulting title
		// at the beginning of it
		if (contentHead) {
			contentHead.children.each { contentHeadChild ->
				def decoratorHeadChildren = decoratorHead.children
				def position = sortingStrategy.findPositionForContent(decoratorHeadChildren, contentHeadChild)
				if (position != -1) {
					decoratorHead.insertChildWithWhitespace(contentHeadChild, position)
				}
			}
		}
		decoratorHead.insertChildWithWhitespace(titleContainer, 0)

		super.decorate(decoratorHead, contentHead)
	}
}
